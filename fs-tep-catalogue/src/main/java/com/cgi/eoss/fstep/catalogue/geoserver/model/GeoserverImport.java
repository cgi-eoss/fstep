package com.cgi.eoss.fstep.catalogue.geoserver.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
@Data
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeoserverImport {

	@JsonProperty(value = "import")
	private Import importField;

	
	
	public GeoserverImport(String targetStore, String targetWorkspace) {
		this.importField = new Import(
				new DatastoreWrapper(new Datastore(targetStore)),
				new WorkspaceWrapper(new Workspace(targetWorkspace))
				);
	}
	
	public GeoserverImport() {
		
	}
	
	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonInclude(Include.NON_NULL)
	public
	class Import {
		private Integer id;
		private String href;
		private DatastoreWrapper targetStore;
		private WorkspaceWrapper targetWorkspace;
	
		public Import() {
			
		}
		
		public Import(DatastoreWrapper targetStore, WorkspaceWrapper targetWorkspace) {
			this.targetStore = targetStore;
			this.targetWorkspace = targetWorkspace;
		}
	}

}



@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
class DatastoreWrapper {
	private final Datastore dataStore;

}

@Data
class WorkspaceWrapper {
	private final Workspace workspace;

}

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
class Datastore {
	private final String name;

}

@Data
class Workspace {
	private final String name;

}
