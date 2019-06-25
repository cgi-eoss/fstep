package com.cgi.eoss.fstep.catalogue.geoserver;

import java.util.HashMap;
import java.util.Map;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GeoServerSpec {

    private String workspace;

    private GeoServerType geoserverType;
    
    private String datastoreName;
    
    private String coverageName;
    
    private String layerName;
    
    private String crs;
    
    private String style;
    
    @Builder.Default
    private Map<String, String> options = new HashMap<>();
}
