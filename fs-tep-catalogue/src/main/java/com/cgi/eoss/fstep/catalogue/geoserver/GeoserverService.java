package com.cgi.eoss.fstep.catalogue.geoserver;

import java.nio.file.Path;
import okhttp3.HttpUrl;

/**
 * <p>Facade to a Geoserver instance, to enhance/enable FS-TEP W*S functionality.</p>
 */
public interface GeoserverService {
    /**
     * <p>Ingest the file at the given path to the selected workspace. The native CRS of the file must be supplied.</p>
     */
    String ingest(String workspace, Path path, String crs);

    String ingest(Path path, GeoServerSpec geoServerSpec);

    boolean isIngestibleFile(String filename);

    /**
     * <p>Delete the layer with the given name from the selected workspace.</p>
     */
    void delete(String workspace, String layerName);

    HttpUrl getExternalUrl();


}
