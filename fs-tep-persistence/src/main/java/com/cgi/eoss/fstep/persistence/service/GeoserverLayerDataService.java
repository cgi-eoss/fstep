package com.cgi.eoss.fstep.persistence.service;

import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.GeoserverLayer;

public interface GeoserverLayerDataService extends
        FstepEntityDataService<GeoserverLayer> {

	void syncGeoserverLayers(FstepFile fstepFile);
}
