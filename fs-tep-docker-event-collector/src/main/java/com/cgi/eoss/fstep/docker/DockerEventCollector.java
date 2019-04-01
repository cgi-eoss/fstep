package com.cgi.eoss.fstep.docker;

import java.io.IOException;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import shadow.dockerjava.com.github.dockerjava.api.DockerClient;
import shadow.dockerjava.com.github.dockerjava.api.command.EventsCmd;
import shadow.dockerjava.com.github.dockerjava.api.model.Event;
import shadow.dockerjava.com.github.dockerjava.api.model.EventType;
import shadow.dockerjava.com.github.dockerjava.core.command.EventsResultCallback;

@Component
public class DockerEventCollector {

	private DockerClient dockerClient;
	
	private JmsTemplate jmsTemplate;
	
	private String brokerQueueName;
	
	@Autowired
	public DockerEventCollector(DockerClient dockerClient, JmsTemplate jmsTemplate, @Value("${brokerQueueName}") String brokerQueueName) {
		this.dockerClient = dockerClient;
		this.jmsTemplate = jmsTemplate;
		this.brokerQueueName = brokerQueueName;
	}
	
	public void collect() throws IOException, InterruptedException {
		EventsCmd eventsCmd = dockerClient.eventsCmd().withSince(String.valueOf(Instant.now().getEpochSecond()));
		eventsCmd.exec(new EventsResultCallback() {
			public void onNext(Event event) {
				processEvent(event);
			}
		}).awaitCompletion().close();
		
	}

	protected void processEvent(Event event) {
		if (event.getType().equals(EventType.CONTAINER)) {
			jmsTemplate.convertAndSend(brokerQueueName, event);
		}
	}
}
