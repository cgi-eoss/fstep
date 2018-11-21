package com.cgi.eoss.fstep.catalogue.files;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import org.geojson.Feature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.cgi.eoss.fstep.catalogue.CatalogueUri;
import com.cgi.eoss.fstep.catalogue.geoserver.GeoServerSpec;
import com.cgi.eoss.fstep.catalogue.geoserver.GeoserverService;
import com.cgi.eoss.fstep.catalogue.resto.RestoService;
import com.cgi.eoss.fstep.catalogue.util.GeoUtil;
import com.cgi.eoss.fstep.model.Collection;
import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.Hashing;
import com.google.common.io.MoreFiles;

import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;

@Component
@Log4j2
public class FilesystemOutputProductService implements OutputProductService {
    private final Path outputProductBasedir;
    private final RestoService resto;
    private final GeoserverService geoserver;
    private final ObjectMapper jsonMapper;

    @Autowired
    public FilesystemOutputProductService(@Qualifier("outputProductBasedir") Path outputProductBasedir, RestoService resto, GeoserverService geoserver, ObjectMapper jsonMapper) {
        this.outputProductBasedir = outputProductBasedir;
        this.resto = resto;
        this.geoserver = geoserver;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public String getDefaultCollection() {
        return resto.getOutputProductsCollection();
    }
    
    @Override
    public FstepFile ingest(String collection, User owner, String jobId, String crs, String geometry, OffsetDateTime startDateTime, OffsetDateTime endDateTime, Map<String, Object> properties, Path src) throws IOException {
        Path dest = outputProductBasedir.resolve(jobId).resolve(src);
        if (!src.equals(dest)) {
            if (Files.exists(dest)) {
                LOG.warn("Found already-existing output product, overwriting: {}", dest);
            }

            Files.createDirectories(dest.getParent());
            Files.move(src, dest, StandardCopyOption.REPLACE_EXISTING);
        }
        LOG.info("Ingesting output at {}", dest);

        if (geoserver.isIngestibleFile(dest.getFileName().toString())) {
            //TODO link geoserver URL to catalogue
            if (properties.containsKey("geoServerSpec")) {
                GeoServerSpec geoServerSpec = (GeoServerSpec) properties.get("geoServerSpec");
                try {
                    geoserver.ingest(dest, geoServerSpec);
                } catch (Exception e) {
                    LOG.error("Failed to ingest output product to GeoServer, continuing...", e);
                }
            }
            else {
                try {
                    geoserver.ingest(jobId, dest, crs);
                } catch (Exception e) {
                    LOG.error("Failed to ingest output product to GeoServer, continuing...", e);
                }
            }
        }
 
        Path relativePath= outputProductBasedir.resolve(jobId).relativize(src);
        
        URI uri = CatalogueUri.OUTPUT_PRODUCT.build(
                ImmutableMap.of(
                        "jobId", jobId,
                        "filename", relativePath.toString().replaceAll(File.pathSeparator, "_")));
        long filesize = Files.size(dest);
        // Add automatically-determined properties
        properties.put("productIdentifier", jobId + "_" + relativePath.toString());
        properties.put("fstepUrl", uri);
        if (startDateTime != null) {
        	properties.put("startDate", startDateTime.toString());
        }
        if (endDateTime != null) {
        	properties.put("completionDate", endDateTime.toString());
        }
        // TODO Get the proper MIME type
        properties.put("resourceMimeType", "application/unknown");
        properties.put("resourceSize", Files.size(dest));
        properties.put("filename", relativePath.toFile().getName());
        properties.put("resourceChecksum", "sha256=" + MoreFiles.asByteSource(dest).hash(Hashing.sha256()));
        // TODO Add extra properties if needed
        properties.put("extraParams", jsonMapper.writeValueAsString(ImmutableMap.of()));

        Feature feature = new Feature();
        feature.setId(jobId + "_" + relativePath.toString().replaceAll(File.pathSeparator, "_"));
        feature.setGeometry(Strings.isNullOrEmpty(geometry) ? GeoUtil.defaultGeometry() : GeoUtil.getGeoJsonGeometry(geometry));
        feature.setProperties(properties);

        UUID restoId;
        try {
            restoId = resto.ingestOutputProduct(collection, feature);
            LOG.info("Ingested output product with Resto id {} and URI {}", restoId, uri);
        } catch (Exception e) {
            LOG.error("Failed to ingest output product to Resto, continuing...", e);
            // TODO Add GeoJSON to FstepFile model
            restoId = UUID.randomUUID();
        }

        FstepFile fstepFile = new FstepFile(uri, restoId);
        fstepFile.setOwner(owner);
        fstepFile.setFilesize(filesize);
        fstepFile.setType(FstepFile.Type.OUTPUT_PRODUCT);
        fstepFile.setFilename(outputProductBasedir.relativize(dest).toString());
        return fstepFile;
    }

    @Override
    public Path provision(String jobId, String filename) throws IOException {
        Path outputPath = outputProductBasedir.resolve(jobId).resolve(filename);
        if (Files.exists(outputPath)) {
            LOG.warn("Found already-existing output product, may be overwritten: {}", outputPath);
        }
        Files.createDirectories(outputPath.getParent());
        return outputPath;
    }

    @Override
    public HttpUrl getWmsUrl(String jobId, String filename) {
        return geoserver.isIngestibleFile(filename)
                ? geoserver.getExternalUrl().newBuilder()
                .addPathSegment(jobId)
                .addPathSegment("wms")
                .addQueryParameter("service", "WMS")
                .addQueryParameter("version", "1.3.0")
                .addQueryParameter("layers", jobId + ":" + MoreFiles.getNameWithoutExtension(Paths.get(filename)))
                .build()
                : null;
    }

    @Override
    public Resource resolve(FstepFile file) {
        Path path = outputProductBasedir.resolve(file.getFilename());
        return new PathResource(path);
    }

    @Override
    public void delete(FstepFile file) throws IOException {
        Path relativePath = Paths.get(file.getFilename());

        Files.deleteIfExists(outputProductBasedir.resolve(relativePath));

        resto.deleteOutputProduct(file.getRestoId());

        // Workspace = jobId = first part of the relative filename
        String workspace = relativePath.getName(0).toString();
        // Layer name = filename without extension
        String layerName = MoreFiles.getNameWithoutExtension(relativePath.getFileName());
        geoserver.delete(workspace, layerName);
    }

    @Override
    public boolean createCollection(Collection collection) {
        return resto.createOutputCollection(collection);
        
    }

    @Override
    public boolean deleteCollection(Collection collection) {
        return resto.deleteCollection(collection);
        
    }

}
