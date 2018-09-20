package com.cgi.eoss.fstep.orchestrator.service;

import lombok.Data;

@Data 
public class ReverseProxyEntry {

	private final String path;
	
	private final String endpoint;
	
}
