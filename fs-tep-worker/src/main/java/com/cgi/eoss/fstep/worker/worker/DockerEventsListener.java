package com.cgi.eoss.fstep.worker.worker;

import shadow.dockerjava.com.github.dockerjava.api.model.Event;

public interface DockerEventsListener {

	public static final String DOCKER_EVENTS_QUEUE = "dockerEvents";
	
	public void receiveDockerEvent(Event event);
}
