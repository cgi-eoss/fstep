package com.cgi.eoss.fstep.catalogue.geoserver.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import lombok.Data;

@Data
@JacksonXmlRootElement(localName = "coverage", namespace = "")
@JsonInclude(Include.NON_NULL)
public class CoverageConfig {
	
	private String name;
	
	private String nativeCoverageName;
	
	private boolean enabled;
	
	private Metadata metadata;
	
	private BoundingBox nativeBoundingBox;
	
	private BoundingBox latLonBoundingBox;
	
}

@Data
class Metadata{
	@JacksonXmlElementWrapper(useWrapping = false)
	private List<Entry> entry = new ArrayList<>();
}

@Data
class Entry{
	@JacksonXmlProperty(isAttribute = true)
	private String key;
	private DimensionInfo dimensionInfo;
}

@Data
class DimensionInfo{
	private boolean enabled;
	
	private String presentation;
	
	private String units;
}

@Data
class BoundingBox{
	private String minx;
	
	private String maxx;
	
	private String miny;
	
	private String maxy;
}