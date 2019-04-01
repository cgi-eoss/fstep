package com.cgi.eoss.fstep.worker.worker;

import java.io.IOException;
import java.time.Instant;

import org.springframework.jms.core.JmsTemplate;

import com.cgi.eoss.fstep.clouds.service.Node;
import com.cgi.eoss.fstep.worker.docker.DockerClientFactory;

import lombok.extern.log4j.Log4j2;
import shadow.dockerjava.com.github.dockerjava.api.DockerClient;
import shadow.dockerjava.com.github.dockerjava.api.command.EventsCmd;
import shadow.dockerjava.com.github.dockerjava.api.model.Event;
import shadow.dockerjava.com.github.dockerjava.api.model.EventType;
import shadow.dockerjava.com.github.dockerjava.core.command.EventsResultCallback;
@Log4j2
public class LocalEventCollectorNodePreparer implements NodePreparer{

	private JmsTemplate jmsTemplate;
	private String brokerQueueName;
	//Ensure a single instance is present
	private static Thread eventCollectorThread;
	
	public LocalEventCollectorNodePreparer(JmsTemplate jmsTemplate, String brokerQueueName) {
		this.jmsTemplate = jmsTemplate;
		this.brokerQueueName = brokerQueueName;
	}
	
	@Override
	public void prepareNode(Node node) {
		if (eventCollectorThread != null) {
    		return;
    	}
    	
    	Runnable eventCollector = () -> {
    		DockerClient dockerClient = DockerClientFactory.buildDockerClient(node.getDockerEngineUrl());
    		EventsCmd eventsCmd = dockerClient.eventsCmd().withSince(String.valueOf(Instant.now().getEpochSecond()));
    		try {
				eventsCmd.exec(new EventsResultCallback() {
					public void onNext(Event event) {
						if (event.getType().equals(EventType.CONTAINER)) {
							jmsTemplate.convertAndSend(brokerQueueName, event);
						}
					}
				}).awaitCompletion().close();
				dockerClient.close();
			} catch (IOException e) {
				LOG.error("Error closing docker events command", e);
			} catch (InterruptedException e) {
				eventCollectorThread.interrupt();
			} finally {
				try {
					dockerClient.close();
				} catch (IOException e) {
					LOG.error("Error releasing docker client", e);
				}
			}
    	};
    	
    	eventCollectorThread = new Thread(eventCollector, "EventCollector");
    	eventCollectorThread.start();
    	
	}

}
