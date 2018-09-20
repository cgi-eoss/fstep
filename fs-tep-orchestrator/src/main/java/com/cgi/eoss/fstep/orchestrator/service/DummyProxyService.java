package com.cgi.eoss.fstep.orchestrator.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import com.cgi.eoss.fstep.rpc.Job;

@Service
@ConditionalOnProperty(value = "fstep.orchestrator.proxy.enabled", havingValue = "false", matchIfMissing = true)
public class DummyProxyService implements DynamicProxyService{

	 @Value("${fstep.orchestrator.gui.baseUrl:}")
	 private String baseUrl;

	 @Value("${fstep.orchestrator.gui.urlPrefix:/gui/}")
	 private String guiUrlPrefix;

	 public ReverseProxyEntry getProxyEntry(Job job, String host, int port) {
		 return new ReverseProxyEntry(baseUrl + guiUrlPrefix + ":" + port + "/", host + ":" + port);
	 }
	 
	 public boolean supportsProxyRoute() {
		 return false;
	 }
	 
	 public String getProxyRoute(Job job) {
		 throw new UnsupportedOperationException();
	 }

	 @Override
	 public void update() {
		//Do nothing;
	}

}
