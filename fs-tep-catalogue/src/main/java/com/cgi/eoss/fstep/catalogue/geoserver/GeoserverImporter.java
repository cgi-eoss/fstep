package com.cgi.eoss.fstep.catalogue.geoserver;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import com.cgi.eoss.fstep.catalogue.IngestionException;
import com.cgi.eoss.fstep.catalogue.geoserver.model.GeoserverImport;
import com.cgi.eoss.fstep.catalogue.geoserver.model.GeoserverImportTask;
import com.cgi.eoss.fstep.catalogue.geoserver.model.GeoserverImportTask.UpdateMode;
import com.cgi.eoss.fstep.catalogue.geoserver.rest.HttpUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.geosolutions.geoserver.rest.manager.GeoServerRESTAbstractManager;

public class GeoserverImporter extends GeoServerRESTAbstractManager {

	private static ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	
    public GeoserverImporter(URL url, String username, String password) {
        super(url, username, password);
    }

    public String createImport(String targetWorkspace, String targetDatastore) throws IOException {
    	GeoserverImport geoserverImport = new GeoserverImport(targetDatastore, targetWorkspace);
    	String sUrl = gsBaseUrl + "/rest/imports/";
    	String sendResult = HttpUtils.post(sUrl, OBJECT_MAPPER.writeValueAsString(geoserverImport), "application/json", gsuser, gspass);
    	GeoserverImport resultImport = OBJECT_MAPPER.readValue(sendResult, GeoserverImport.class);
    	return resultImport.getImportField().getHref();
    }
    
  
    public String addShapeFileToImport(Path shapeFile, String importUrl) throws IOException {
    	String sUrl = importUrl + "/tasks";
    	Part[] parts = new Part[2];
    	parts[0] = new StringPart("name",  shapeFile.toFile().getName());
    	parts[1] = new FilePart ("filedata",  shapeFile.toFile());
    	PostMethod postMethod = new PostMethod(sUrl);
    	postMethod.addRequestHeader("Content-Type", "multipart/form-data");
    	MultipartRequestEntity request = new MultipartRequestEntity(parts, postMethod.getParams());
    	String sendResult = HttpUtils.post(sUrl, request, gsuser, gspass);
    	GeoserverImportTask resultImportTask = OBJECT_MAPPER.readValue(sendResult, GeoserverImportTask.class);
    	return resultImportTask.getTask().getHref();
    }

	public boolean existsFeatureType(String workspace, String store, String layerName) {
		String sUrl = gsBaseUrl + "/rest/workspaces/" + workspace + "/datastores/" + store + "/featuretypes/" + layerName + ".json";
		String sendResult = HttpUtils.get(sUrl, gsuser, gspass);
		return sendResult != null;
    	
	}

	public void setTaskUpdateMode(String taskUrl, UpdateMode updateMode) throws JsonProcessingException {
		GeoserverImportTask task = new GeoserverImportTask();
		task.getTask().setUpdateMode(UpdateMode.APPEND);
		String result = HttpUtils.put(taskUrl, OBJECT_MAPPER.writeValueAsString(task), "application/json", gsuser, gspass);
		if (result == null) {
            		throw new IngestionException("Unsuccessful change to task update mode");
        	}
	}

	public void runImport(String importUrl) {
	    String result = HttpUtils.post(importUrl, null, gsuser, gspass);
	    if (result == null) {
	        throw new IngestionException("Import was not successful");
	    }
	}
}
