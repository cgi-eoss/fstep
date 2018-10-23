package com.cgi.eoss.fstep.search.clms.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.geojson.Feature;
import org.geojson.LngLatAlt;
import org.geojson.Polygon;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CLMSProduct {
	
	 @JsonProperty("identifier")
	 private String identifier;
	 
	 @JsonProperty("id")
	 private String id;
	 
	 @JsonProperty("polygon")
	 private String polygon;
	 
	 private String beginPosition;
	 
	 private String endPosition;

	 private String downloadLink;
	 
	 private String quicklookLink;
	 
	@JsonProperty("EarthObservation")
	@SuppressWarnings("unchecked")
	private void unpackNested(Map<String, Object> earthObservation) {
		Map<String, Object> phenomenonTime =  (Map<String, Object>) earthObservation.get("phenomenonTime");
		Map<String, String> timePeriod =  (Map<String, String>) phenomenonTime.get("TimePeriod");
		this.beginPosition = timePeriod.get("beginPosition");
		this.endPosition = timePeriod.get("endPosition");
		Map<String, Object> result =  (Map<String, Object>) earthObservation.get("result");
		Map<String, Object> earthObservationResult =  (Map<String, Object>) result.get("EarthObservationResult");
		Map<String, Object> product =  (Map<String, Object>) earthObservationResult.get("product");
		Map<String, Object> productInformation =  (Map<String, Object>) product.get("ProductInformation");
		Map<String, Object> fileName =  (Map<String, Object>) productInformation.get("fileName");
		Map<String, Object> serviceReference =  (Map<String, Object>) fileName.get("ServiceReference");
		this.downloadLink =  (String) serviceReference.get("href");
		
		Map<String, Object> browse =  (Map<String, Object>) earthObservationResult.get("browse");
		Map<String, Object> browseInformation =  (Map<String, Object>) browse.get("BrowseInformation");
		Map<String, Object> browseFileName =  (Map<String, Object>) browseInformation.get("fileName");
		Map<String, Object> browseServiceReference =  (Map<String, Object>) browseFileName.get("ServiceReference");
		this.quicklookLink =  (String) browseServiceReference.get("href");
		
	}

	 
	 public Feature asFeature() {
	    Feature f = new Feature();
	    f.setId(id);
		f.setProperty("identifier", identifier);
		Polygon p = asPolygon(polygon);
		f.setGeometry(p);
		f.setProperty("beginPosition", beginPosition);
		f.setProperty("endPosition", endPosition);
		f.setProperty("quicklookLink", quicklookLink);
		return f;
	}


	private Polygon asPolygon(String polygon) {
		StringTokenizer st = new StringTokenizer(polygon);
		List<LngLatAlt> points = new ArrayList<LngLatAlt>();
		while (st.hasMoreTokens()) {
			double lat = Double.parseDouble(st.nextToken());
			double lon = Double.parseDouble(st.nextToken());
			points.add(new LngLatAlt(lon, lat));
		}
		return new Polygon(points);
	}
}