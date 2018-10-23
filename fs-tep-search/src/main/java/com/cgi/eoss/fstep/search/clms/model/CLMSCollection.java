package com.cgi.eoss.fstep.search.clms.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import lombok.Data;
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CLMSCollection {
	 @JacksonXmlProperty(namespace = "http://www.w3.org/2005/Atom")
	 @JsonProperty("title")
	 private String title;
	 @JacksonXmlProperty(namespace = "http://www.w3.org/2005/Atom")
	 @JsonProperty("summary")
	 private String summary;

}
