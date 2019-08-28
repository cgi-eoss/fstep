package com.cgi.eoss.fstep.orchestrator.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cgi.eoss.fstep.catalogue.geoserver.GeoServerSpec;
import com.cgi.eoss.fstep.model.FstepServiceDescriptor.Parameter;
import com.cgi.eoss.fstep.model.Job;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.collect.Iterables;
import com.mysema.commons.lang.Pair;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class PlatformParameterExtractor {

	private static final String COLLECTION_PARAM = "collection";
	private static final String GEO_SERVER_SPEC_PARAM = "geoServerSpec";
	private static final String TIMEOUT_PARAM = "timeout";

	private PlatformParameterExtractor() {
		
	}
	
	public static Map<String, GeoServerSpec> getGeoServerSpecs(Job job) throws IOException {
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
    
    public static Map<String, String> getCollectionSpecs(Job job) throws IOException {
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
    
    public static Integer getTimeout(Job job) {
    	return Integer.valueOf(Iterables.getOnlyElement(job.getConfig().getInputs().get(TIMEOUT_PARAM), "0"));
    }
    
    public static Pair<OffsetDateTime, OffsetDateTime> extractStartEndDateTimes(Parameter outputParameter, String value) {
		try {
			String regexp = outputParameter.getTimeRegexp();
			if (regexp != null) {
				Pattern p = Pattern.compile(regexp);
				Matcher m = p.matcher(value);
				if (m.find()) {
					if (regexp.contains("?<startEnd>")) {
						OffsetDateTime startEndDateTime = parseOffsetDateTime(m.group("startEnd"), LocalTime.MIDNIGHT);
						return new Pair<>(startEndDateTime, startEndDateTime);
					} else {
						OffsetDateTime start = null;
						OffsetDateTime end = null;
						if (regexp.contains("?<start>")) {
							start = parseOffsetDateTime(m.group("start"), LocalTime.MIDNIGHT);
						}

						if (regexp.contains("?<end>")) {
							end = parseOffsetDateTime(m.group("end"), LocalTime.MIDNIGHT);
						}
						return new Pair<>(start, end);
					}
				}
			}
		} catch (RuntimeException e) {
			LOG.error("Unable to parse date from regexp");
		}
		return new Pair<>(null, null);
	}

	private static OffsetDateTime parseOffsetDateTime(String startDateStr, LocalTime defaultTime) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd[[ ]['T']HHmm[ss][.SSS][XXX]]");
		TemporalAccessor temporalAccessor = formatter.parseBest(startDateStr, OffsetDateTime::from, LocalDateTime::from,
				LocalDate::from);
		if (temporalAccessor instanceof OffsetDateTime) {
			return (OffsetDateTime) temporalAccessor;
		} else if (temporalAccessor instanceof LocalDateTime) {
			return ((LocalDateTime) temporalAccessor).atOffset(ZoneOffset.UTC);
		} else {
			return ((LocalDate) temporalAccessor).atTime(defaultTime).atOffset(ZoneOffset.UTC);
		}
	}
}
