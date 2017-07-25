package com.cgi.eoss.fstep.catalogue.external;

import com.cgi.eoss.fstep.catalogue.FstepFileService;
import com.cgi.eoss.fstep.model.FstepFile;
import org.geojson.GeoJsonObject;

public interface ExternalProductDataService extends FstepFileService {
    FstepFile ingest(GeoJsonObject geoJson);
}
