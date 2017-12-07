package com.cgi.eoss.fstep.catalogue.geoserver;

import static it.geosolutions.geoserver.rest.encoder.GSResourceEncoder.ProjectionPolicy.REPROJECT_TO_DECLARED;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.cgi.eoss.fstep.catalogue.IngestionException;
import com.google.common.io.MoreFiles;
import it.geosolutions.geoserver.rest.GeoServerRESTManager;
import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;
import it.geosolutions.geoserver.rest.GeoServerRESTReader;
import it.geosolutions.geoserver.rest.decoder.RESTCoverageStore;
import it.geosolutions.geoserver.rest.encoder.GSLayerEncoder;
import it.geosolutions.geoserver.rest.encoder.GSResourceEncoder;
import it.geosolutions.geoserver.rest.encoder.coverage.GSCoverageEncoder;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;

@Component
@Log4j2
public class GeoserverServiceImpl implements GeoserverService {

    private static final String RASTER_STYLE = "raster";

    @Getter
    private final HttpUrl externalUrl;
    private final GeoServerRESTPublisher publisher;
    private final GeoServerRESTReader reader;

    @Value("${fstep.catalogue.geoserver.enabled:true}")
    private boolean geoserverEnabled;

    @Value("#{'${fstep.catalogue.geoserver.ingest-filetypes:TIF}'.split(',')}")
    private Set<String> ingestableFiletypes;

    private GeoserverMosaicUpdater mosaicUpdater;

    @Autowired
    public GeoserverServiceImpl(@Value("${fstep.catalogue.geoserver.url:http://fstep-geoserver:9080/geoserver/}") String url,
            @Value("${fstep.catalogue.geoserver.externalUrl:http://fstep-geoserver:9080/geoserver/}") String externalUrl,
            @Value("${fstep.catalogue.geoserver.username:fstepgeoserver}") String username,
            @Value("${fstep.catalogue.geoserver.password:fstepgeoserverpass}") String password) throws MalformedURLException {
        this.externalUrl = HttpUrl.parse(externalUrl);
        GeoServerRESTManager geoserver = new GeoServerRESTManager(new URL(url), username, password);
        this.publisher = geoserver.getPublisher();
        this.mosaicUpdater = new GeoserverMosaicUpdater(new URL(url), username, password);
        this.reader = geoserver.getReader();
    }

    @Override
    public String ingest(String workspace, Path path, String crs) {
        Path fileName = path.getFileName();
        String datastoreName = MoreFiles.getNameWithoutExtension(fileName);
        String layerName = MoreFiles.getNameWithoutExtension(fileName);
        
        return ingestCoverage(workspace, path, crs, datastoreName, layerName, RASTER_STYLE);
    }
    
    private String ingestCoverage(String workspace, Path path, String crs, String datastoreName, String layerName, String style) {
        Path fileName = path.getFileName();
        if (!geoserverEnabled) {
            LOG.warn("Geoserver is disabled; 'ingested' file: {}:{}", workspace, layerName);
            return null;
        }

        ensureWorkspaceExists(workspace);

        if (!isIngestibleFile(fileName.toString())) {
            // TODO Ingest more filetypes
            LOG.info("Unable to ingest product with filename: {}", fileName);
            return null;
        }

        try {
            RESTCoverageStore restCoverageStore = publishExternalGeoTIFF(workspace, datastoreName, path.toFile(), layerName, crs,
                    REPROJECT_TO_DECLARED, style);
            LOG.info("Ingested GeoTIFF to geoserver with id: {}:{}", workspace, layerName);
            return restCoverageStore.getURL();
        } catch (FileNotFoundException e) {
            LOG.error("Geoserver was unable to publish file: {}", path, e);
            throw new IngestionException(e);
        }
    }
    
    private String ingestCoverageInMosaic(String workspace, Path path, String crs, String datastoreName, String layerName, String style) {
        Path fileName = path.getFileName();
        if (!geoserverEnabled) {
            LOG.warn("Geoserver is disabled; 'ingested' file: {}:{}", workspace, layerName);
            return null;
        }

        ensureWorkspaceExists(workspace);

        if (!isIngestibleFile(fileName.toString())) {
            // TODO Ingest more filetypes
            LOG.info("Unable to ingest product with filename: {}", fileName);
            return null;
        }

        try {
            RESTCoverageStore restCoverageStore = publishExternalGeoTIFFToMosaic(workspace, datastoreName, path.toFile());
            LOG.info("Ingested GeoTIFF to geoserver with id: {}:{}", workspace, layerName);
            LOG.info("Reloading GeoServer configuration");
            publisher.reload();
            return restCoverageStore.getURL();
        } catch (FileNotFoundException e) {
            LOG.error("Geoserver was unable to publish file: {}", path, e);
            throw new IngestionException(e);
        }
    }

    @Override
    public String ingest(Path path, GeoServerSpec geoServerSpec) {
        String workspace = geoServerSpec.getWorkspace();
        String datastoreName = geoServerSpec.getDatastoreName();
        String coverageName = geoServerSpec.getCoverageName();
        String srs = geoServerSpec.getCrs();
        String style = geoServerSpec.getStyle();
        
        switch (geoServerSpec.getGeoserverType()) {
            case SINGLE_COVERAGE: return ingestCoverage(workspace, path, srs, datastoreName, coverageName, style); 
            case MOSAIC: return ingestCoverageInMosaic(workspace, path, srs, datastoreName, coverageName, style); 
            default: throw new IngestionException("GeoServerType not specified");
        }
    }


    @Override
    public boolean isIngestibleFile(String filename) {
        return ingestableFiletypes.stream().anyMatch(ft -> filename.toUpperCase().endsWith("." + ft.toUpperCase()));
    }

    @Override
    public void delete(String workspace, String layerName) {
        if (!geoserverEnabled) {
            LOG.warn("Geoserver is disabled; no deletion occurring for {}:{}", workspace, layerName);
            return;
        }

        publisher.removeLayer(workspace, layerName);
        LOG.info("Deleted layer from geoserver: {}{}", workspace, layerName);
    }

    private void ensureWorkspaceExists(String workspace) {
        if (!reader.existsWorkspace(workspace)) {
            LOG.info("Creating new workspace {}", workspace);
            publisher.createWorkspace(workspace);
        }
    }

    private RESTCoverageStore publishExternalGeoTIFF(String workspace, String storeName, File geotiff, String coverageName, String srs,
            GSResourceEncoder.ProjectionPolicy policy, String defaultStyle) throws FileNotFoundException, IllegalArgumentException {
        if (workspace == null || storeName == null || geotiff == null || coverageName == null || srs == null || policy == null
                || defaultStyle == null)
            throw new IllegalArgumentException("Unable to run: null parameter");

        // config coverage props (srs)
        final GSCoverageEncoder coverageEncoder = new GSCoverageEncoder();
        coverageEncoder.setName(coverageName);
        coverageEncoder.setTitle(coverageName);
        coverageEncoder.setSRS(srs);
        coverageEncoder.setProjectionPolicy(policy);

        // config layer props (style, ...)
        final GSLayerEncoder layerEncoder = new GSLayerEncoder();
        layerEncoder.setDefaultStyle(defaultStyle);

        return publisher.publishExternalGeoTIFF(workspace, storeName, geotiff, coverageEncoder, layerEncoder);
    }
    
    private RESTCoverageStore publishExternalGeoTIFFToMosaic(String workspace, String storeName, File geotiff) throws FileNotFoundException, IllegalArgumentException {
        if (workspace == null || storeName == null || geotiff == null)
            throw new IllegalArgumentException("Unable to run: null parameter");
        RESTCoverageStore response = mosaicUpdater.addGeoTiffToExternalMosaic(workspace, storeName, geotiff);
        return response;
    }

}
