package com.cgi.eoss.fstep.search.clms.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import lombok.Data;

@Data
@JacksonXmlRootElement(localName = "feed", namespace = "http://www.w3.org/2005/Atom")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Feed {
	
	@JacksonXmlProperty(namespace = "http://www.w3.org/2005/Atom")
	@JsonProperty("title")
	private String title;
	 
	
	@JacksonXmlElementWrapper(localName = "entry", namespace= "http://www.w3.org/2005/Atom",useWrapping=false)
	@JacksonXmlProperty(localName = "entry", namespace = "http://www.w3.org/2005/Atom")
    private List<CLMSCollection> collections;
}
