package com.cgi.eoss.fstep.catalogue.geoserver;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import it.geosolutions.geoserver.rest.HTTPUtils;
import it.geosolutions.geoserver.rest.decoder.RESTCoverageStore;
import it.geosolutions.geoserver.rest.manager.GeoServerRESTAbstractManager;

public class GeoserverMosaicUpdater extends GeoServerRESTAbstractManager {


    public GeoserverMosaicUpdater(URL url, String username, String password) {
        super(url, username, password);
    }

    public RESTCoverageStore addGeoTiffToExternalMosaic(String workspace, final String storeName, File geoTiff) throws FileNotFoundException, IllegalArgumentException {

        RESTCoverageStore store = postGeoTiffToExternalMosaic(workspace, storeName, geoTiff);

        return store;
    }

    private RESTCoverageStore postGeoTiffToExternalMosaic(String workspace, String storeName, File geoTiff) throws FileNotFoundException {

        String sUrl = gsBaseUrl + "/rest/workspaces/" + workspace + "/coveragestores/" + storeName + "/external.imagemosaic";
        String sendResult = HTTPUtils.post(sUrl, geoTiff.toURI().toString(), "text/plain", gsuser, gspass);
        return RESTCoverageStore.build(sendResult);
    }
}
