package com.cgi.eoss.fstep.queues.service;

public interface BrowserClient {

	public void handleMessage(Message m);
	
	public boolean stopBrowsing();
}
