package com.cgi.eoss.fstep.docker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class EventCollectorRunner implements CommandLineRunner{

	@Autowired
	DockerEventCollector dockerEventCollector;

    @Override
    public void run(String...args) throws Exception {
    	LOG.info("Start of event collection");
    	dockerEventCollector.collect();
    }
	
}
