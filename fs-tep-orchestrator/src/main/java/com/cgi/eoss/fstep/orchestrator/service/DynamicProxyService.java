package com.cgi.eoss.fstep.orchestrator.service;

import com.cgi.eoss.fstep.rpc.Job;

public interface DynamicProxyService {
	
	public ReverseProxyEntry getProxyEntry(Job job, String host, int port);
	
	public default boolean supportsProxyRoute() {
		return false;
	}
	
	public String getProxyRoute(Job job);
	
	public void update();
}
