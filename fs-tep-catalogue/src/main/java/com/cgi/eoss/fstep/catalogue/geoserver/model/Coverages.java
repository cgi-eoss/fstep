package com.cgi.eoss.fstep.catalogue.geoserver.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import lombok.Data;
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "coverages", namespace = "")
public class Coverages {
	
	@JacksonXmlElementWrapper(useWrapping = false)
    private List<Coverage> coverage = new ArrayList<>();    
}

