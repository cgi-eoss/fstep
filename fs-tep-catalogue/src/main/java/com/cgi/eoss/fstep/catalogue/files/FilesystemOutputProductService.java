package com.cgi.eoss.fstep.catalogue.files;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.geojson.Feature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.cgi.eoss.fstep.catalogue.CatalogueUri;
import com.cgi.eoss.fstep.catalogue.geoserver.GeoServerSpec;
import com.cgi.eoss.fstep.catalogue.geoserver.GeoServerType;
import com.cgi.eoss.fstep.catalogue.geoserver.GeoserverService;
import com.cgi.eoss.fstep.catalogue.resto.RestoService;
import com.cgi.eoss.fstep.catalogue.util.GeoUtil;
import com.cgi.eoss.fstep.catalogue.util.GeometryException;
import com.cgi.eoss.fstep.model.Collection;
import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.GeoserverLayer;
import com.cgi.eoss.fstep.model.User;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.Hashing;
import com.google.common.io.MoreFiles;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class FilesystemOutputProductService implements OutputProductService {
	private static final String DEFAULT_OUTPUTS_GEOSERVER_WORKSPACE = "fs-outputs";
	
    private final Path outputProductBasedir;
    private final RestoService resto;
    private final GeoserverService geoserver;
	private boolean useGeoServerDefaultIngestions;
	private final OGCLinkService ogcLinkService;
    
    @Autowired
    public FilesystemOutputProductService(@Qualifier("outputProductBasedir") Path outputProductBasedir, RestoService resto, GeoserverService geoserver, @Value("${fstep.outputs.useGeoserverDefaultIngestion:true}") boolean useGeoServerDefaultIngestions, OGCLinkService ogcLinkService) {
        this.outputProductBasedir = outputProductBasedir;
        this.resto = resto;
        this.geoserver = geoserver;
        this.useGeoServerDefaultIngestions = useGeoServerDefaultIngestions;
        this.ogcLinkService = ogcLinkService;
    }

    @Override
    public String getDefaultCollection() {
        return resto.getOutputProductsCollection();
    }
    
    @Override
    public FstepFile ingest(String collection, User owner, String jobId, String crs, String geometry, OffsetDateTime startDateTime, OffsetDateTime endDateTime, Map<String, Object> properties, Path src) throws IOException {
        Path dest = outputProductBasedir.resolve(jobId).resolve(src);
        if (!src.equals(dest)) {
            if (dest.toFile().exists()) {
                LOG.warn("Found already-existing output product, overwriting: {}", dest);
            }

            Files.createDirectories(dest.getParent());
            Files.move(src, dest, StandardCopyOption.REPLACE_EXISTING);
        }
        LOG.info("Ingesting output at {}", dest);
 
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
        Map<String, Object> extraProperties;
        if ((properties.get("extraParams") == null)) {
            // TODO Add local extra properties if needed
            extraProperties = ImmutableMap.of();

        } else {
            Map<String, Object> existingExtraProperties = (Map<String, Object>) properties.get("extraParams");
            extraProperties = new HashMap<>();
            extraProperties.putAll(existingExtraProperties);
        }
        properties.put("extraParams", extraProperties);

        Feature feature = buildRestoFeature(jobId, geometry, properties, relativePath);

        UUID restoId = ingestIntoResto(collection, uri, feature);
        
        GeoServerSpec geoServerSpec = null;
        if (properties.containsKey("geoServerSpec")) {
            geoServerSpec = (GeoServerSpec) properties.get("geoServerSpec");
        }

        else if (useGeoServerDefaultIngestions){
        	geoServerSpec = getDefaultGeoServerSpec(dest, crs);
        }
       
    	FstepFile fstepFile = new FstepFile(uri, restoId);
        fstepFile.setOwner(owner);
        fstepFile.setFilesize(filesize);
        fstepFile.setType(FstepFile.Type.OUTPUT_PRODUCT);
        fstepFile.setFilename(outputProductBasedir.relativize(dest).toString());
        if (geoServerSpec != null) {
        	GeoserverLayer geoserverLayer = ingestIntoGeoserver(owner, dest, restoId, geoServerSpec);
        	if (geoserverLayer != null) {
            	fstepFile.getGeoserverLayers().add(geoserverLayer);
            }
        }
       
        return fstepFile;
    }

	private Feature buildRestoFeature(String jobId, String geometry, Map<String, Object> properties,
			Path relativePath) {
		Feature feature = new Feature();
        feature.setId(jobId + "_" + relativePath.toString().replaceAll(File.pathSeparator, "_"));
        if (Strings.isNullOrEmpty(geometry)) {
        	feature.setGeometry(GeoUtil.defaultGeometry());
        }
        else {
        	try {
        		feature.setGeometry(GeoUtil.getGeoJsonGeometry(geometry));
        	}
        	catch (GeometryException e) {
        		feature.setGeometry(GeoUtil.defaultGeometry());
        	}
        }
        feature.setProperties(properties);
		return feature;
	}

	
	private GeoServerSpec getDefaultGeoServerSpec(Path dest, String crs) {
		if (StringUtils.startsWithIgnoreCase(MoreFiles.getFileExtension(dest), "TIF")){
			Path relativePath = outputProductBasedir.relativize(dest);
			Path relativePathWithoutExtension = relativePath.getParent().resolve(MoreFiles.getNameWithoutExtension(dest.getFileName()));
			String coverageName = relativePathWithoutExtension.toString().replace("/", "_");
			return GeoServerSpec.builder()
			.geoserverType(GeoServerType.SINGLE_COVERAGE)
			.workspace(DEFAULT_OUTPUTS_GEOSERVER_WORKSPACE)
			.datastoreName(coverageName)
			.coverageName(coverageName)
			.crs(crs)
			.build();
		}
		return null;
	}

	private UUID ingestIntoResto(String collection, URI uri, Feature feature) {
		UUID restoId;
        try {
            restoId = resto.ingestOutputProduct(collection, feature);
            LOG.info("Ingested output product with Resto id {} and URI {}", restoId, uri);
        } catch (Exception e) {
            LOG.error("Failed to ingest output product to Resto, continuing...", e);
            // TODO Add GeoJSON to FstepFile model
            restoId = UUID.randomUUID();
        }
		return restoId;
	}
	
	private GeoserverLayer ingestIntoGeoserver(User owner, Path dest, UUID restoId, GeoServerSpec geoServerSpec) {
		try {
			GeoserverLayer geoserverLayer = geoserver.ingest(dest, geoServerSpec, restoId);
			geoserverLayer.setOwner(owner);
			return geoserverLayer;
		}
		catch (Exception e) {
            LOG.error("Failed to ingest output product to GeoServer, continuing...", e);
            return null;
        }
	}

    @Override
    public Path provision(String jobId, String filename) throws IOException {
        Path outputPath = outputProductBasedir.resolve(jobId).resolve(filename);
        if (outputPath.toFile().exists()) {
            LOG.warn("Found already-existing output product, may be overwritten: {}", outputPath);
        }
        Files.createDirectories(outputPath.getParent());
        return outputPath;
    }

    @Override
    public Set<Link> getOGCLinks(FstepFile fstepFile) {
        return ogcLinkService.getOGCLinks(fstepFile);
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
        String collection = null;
        if (file.getCollection() != null) {
        	collection = file.getCollection().getIdentifier();
        }
        resto.deleteOutputProduct(collection, file.getRestoId());
        for (GeoserverLayer geoserverLayer: file.getGeoserverLayers()) {
            geoserver.cleanUpGeoserverLayer(file.getFilename(), geoserverLayer);
        }
    }
    
    

    @Override
    public void createCollection(Collection collection) throws IOException{
        if(!resto.createOutputCollection(collection)) {
        	throw new IOException("Failed to create the underlying collection");
        }
        
    }

    @Override
    public void deleteCollection(Collection collection) throws IOException{
    	if(!resto.deleteCollection(collection)) {
        	throw new IOException("Failed to delete the underlying collection");
        }
    }

}
