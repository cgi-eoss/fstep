package com.cgi.eoss.fstep.orchestrator.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.cgi.eoss.fstep.catalogue.geoserver.GeoServerSpec;
import com.cgi.eoss.fstep.model.Job;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.collect.Iterables;

public class PlatformParameterExtractor {

	private static final String COLLECTION_PARAM = "collection";
	private static final String GEO_SERVER_SPEC_PARAM = "geoServerSpec";
	private static final String TIMEOUT_PARAM = "timeout";

	public Map<String, GeoServerSpec> getGeoServerSpecs(Job job) throws IOException {
        String geoServerSpecsStr = Iterables.getOnlyElement(job.getConfig().getInputs().get(GEO_SERVER_SPEC_PARAM), null);
        Map<String, GeoServerSpec> geoServerSpecs = new HashMap<>();
        if (geoServerSpecsStr != null && geoServerSpecsStr.length() > 0) {
            ObjectMapper mapper = new ObjectMapper();
                TypeFactory typeFactory = mapper.getTypeFactory();
                MapType mapType = typeFactory.constructMapType(HashMap.class, String.class, GeoServerSpec.class);
                geoServerSpecs.putAll(mapper.readValue(geoServerSpecsStr, mapType));
        }
        return geoServerSpecs;
    }
    
    public Map<String, String> getCollectionSpecs(Job job) throws IOException {
        String collectionsStr = Iterables.getOnlyElement(job.getConfig().getInputs().get(COLLECTION_PARAM), null);
        Map<String, String> collectionSpecs = new HashMap<>();
        if (collectionsStr != null && collectionsStr.length() > 0) {
            ObjectMapper mapper = new ObjectMapper();
                TypeFactory typeFactory = mapper.getTypeFactory();
                MapType mapType = typeFactory.constructMapType(HashMap.class, String.class, String.class);
                collectionSpecs.putAll(mapper.readValue(collectionsStr, mapType));
        }
        return collectionSpecs;
    }
    
    public Integer getTimeout(Job job) {
    	return Integer.valueOf(Iterables.getOnlyElement(job.getConfig().getInputs().get(TIMEOUT_PARAM), "0"));
    }
}
