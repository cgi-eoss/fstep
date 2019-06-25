package com.cgi.eoss.fstep.catalogue.geoserver;

import java.nio.file.Path;
import java.util.UUID;

import com.cgi.eoss.fstep.model.GeoserverLayer;

import okhttp3.HttpUrl;

/**
 * <p>Facade to a Geoserver instance, to enhance/enable FS-TEP W*S functionality.</p>
 */
public interface GeoserverService {
    
	/**
     * <p>Ingests the file in geoserver according to the geoserverSpec</p>
     */
    GeoserverLayer ingest(Path path, GeoServerSpec geoServerSpec, UUID id);
    
    boolean isIngestibleFile(String filename);
    
    /**
     * <p>Delete the layer with the given name from the selected workspace.</p>
     */
    void deleteLayer(String workspace, String layerName);

    /**
     * <p>Delete the coverage with the given name from the selected workspace.</p>
     */
    void unpublishCoverage(String workspace, String storeName, String layerName);

    /**
     * <p>Delete the coverage store with the given name from the selected workspace.</p>
     */
    void deleteCoverageStore(String workspace, String storeName);
    
    HttpUrl getExternalUrl();

    void createEmptyMosaic(String workspace, String storeName, String coverageName, String timeRegexp);

    void deleteGranuleFromMosaic(String workspace, String storeName, String coverageName, String location);

	void cleanUpGeoserverLayer(String path, GeoserverLayer geoserverLayer);

}
