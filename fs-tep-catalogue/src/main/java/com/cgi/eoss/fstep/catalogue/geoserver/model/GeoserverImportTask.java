package com.cgi.eoss.fstep.catalogue.geoserver.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class GeoserverImportTask {

	@JsonProperty(value = "task")
	private Task task;
	
	public GeoserverImportTask() {
		this.task = new Task();
	}
	
	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonInclude(Include.NON_NULL)
	public class Task {
		private Integer id;
		private String href;
		private UpdateMode updateMode;
	}

	public enum UpdateMode{
		REPLACE, CREATE, APPEND;
	}
}



