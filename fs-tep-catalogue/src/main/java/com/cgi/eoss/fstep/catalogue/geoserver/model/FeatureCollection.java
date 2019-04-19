package com.cgi.eoss.fstep.catalogue.geoserver.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FeatureCollection {

    private List<Feature> features = new ArrayList<>();  
	
}
