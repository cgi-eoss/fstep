package com.cgi.eoss.fstep.worker.worker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.cgi.eoss.fstep.clouds.service.Node;
import com.cgi.eoss.fstep.worker.DockerRegistryConfig;
import com.cgi.eoss.fstep.worker.docker.DockerClientFactory;

import lombok.extern.log4j.Log4j2;
import shadow.dockerjava.com.github.dockerjava.api.DockerClient;
import shadow.dockerjava.com.github.dockerjava.api.command.CreateContainerCmd;
import shadow.dockerjava.com.github.dockerjava.api.model.Bind;
import shadow.dockerjava.com.github.dockerjava.api.model.Container;
@Log4j2
public class DockerEventCollectorNodePreparer implements NodePreparer{


	private DockerRegistryConfig dockerRegistryConfig;
	private String imageName;
	private String brokerUrl;
	private String brokerUsername;
	private String brokerPassword;
	private String brokerQueueName;
	
	public DockerEventCollectorNodePreparer(DockerRegistryConfig dockerRegistryConfig, String imageName, String brokerUrl, String brokerUsername, String brokerPassword, String brokerQueueName) {
		this.dockerRegistryConfig = dockerRegistryConfig;
		this.imageName = imageName;
		this.brokerUrl = brokerUrl;
		this.brokerUsername = brokerUsername;
		this.brokerPassword = brokerPassword;
		this.brokerQueueName = brokerQueueName;
	}
	
	@Override
	public void prepareNode(Node node) {
		DockerClient dockerClient;
    	if (dockerRegistryConfig != null) {
    		dockerClient = DockerClientFactory.buildDockerClient(node.getDockerEngineUrl(), dockerRegistryConfig);
    	}
    	else {
    		dockerClient = DockerClientFactory.buildDockerClient(node.getDockerEngineUrl());
    	}
    	List<String> environmentVariables = new ArrayList<>();
    	environmentVariables.add("dockerUrl=" + node.getDockerEngineUrl());
    	environmentVariables.add("brokerUrl=" + brokerUrl);
    	environmentVariables.add("brokerUsername=" + brokerUsername);
    	environmentVariables.add("brokerPassword=" + brokerPassword);
    	environmentVariables.add("brokerQueueName=" + brokerQueueName);
    	Map<String,String> labels = new HashMap<>();
    	labels.put("app", "docker-event-collector");
		
    	List<Container> existingContainers = dockerClient.listContainersCmd().withShowAll(true).withLabelFilter(labels).exec();
    	for (Container c: existingContainers) {
    		if (!c.getStatus().startsWith("Exited")) {
    			dockerClient.stopContainerCmd(c.getId()).exec();
    		}
    		dockerClient.removeContainerCmd(c.getId()).exec();
    	}
    	try (CreateContainerCmd createContainerCmd =  dockerClient.createContainerCmd(imageName)
    			.withBinds(Bind.parse("/var/run/docker.sock:/var/run/docker.sock"))
    			.withEnv(environmentVariables)
    			.withName("docker-event-collector")
    			.withLabels(labels)){
    		String containerId = createContainerCmd.exec().getId();
    		dockerClient.startContainerCmd(containerId).exec();
    	}
		try {
			TimeUnit.SECONDS.sleep(10);
		} catch (InterruptedException e) {
			LOG.error("Docker Event thread interrupted");
			Thread.currentThread().interrupt();
			
		}
	}

}
