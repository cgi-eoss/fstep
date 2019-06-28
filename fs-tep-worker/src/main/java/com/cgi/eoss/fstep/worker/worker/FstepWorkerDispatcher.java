package com.cgi.eoss.fstep.worker.worker;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.cgi.eoss.fstep.queues.service.FstepQueueService;
import com.cgi.eoss.fstep.rpc.JobSpec;

import lombok.extern.log4j.Log4j2;
import shadow.dockerjava.com.github.dockerjava.api.model.Event;
import shadow.dockerjava.com.github.dockerjava.api.model.EventType;

/**
 * <p>
 * Service for executing FS-TEP (WPS) services inside Docker containers.
 * </p>
 */
@Log4j2
@Service
public class FstepWorkerDispatcher implements DockerEventsListener{

    private FstepQueueService queueService;
    
    private static final long QUEUE_SCHEDULER_INTERVAL_MS = 10L * 1000L;

    
    
    private JobExecutionController jobExecutionController;
    
    @Autowired
    public FstepWorkerDispatcher(FstepQueueService queueService, JobExecutionController jobExecutionController) {
        this.queueService = queueService;
        this.jobExecutionController = jobExecutionController;
    }


    @Scheduled(fixedDelay = QUEUE_SCHEDULER_INTERVAL_MS, initialDelay = 10000L)
    public void getNewJobs() {
        while (jobExecutionController.hasCapacity()) {
            LOG.debug("Checking for available jobs in the queue");
            JobSpec nextJobSpec = (JobSpec) queueService.receiveObjectNoWait(FstepQueueService.jobExecutionQueueName);
            if (nextJobSpec != null) {
                LOG.info("Dequeued job {}", nextJobSpec.getJob().getId());
                if (!jobExecutionController.acceptJob(nextJobSpec)) {
                	//TODO This shouldn't happen due to the capacity check above,
                	//but it would be better to not acknowledge the message: anyway, we are not using message acknowledgement at this stage
                }
            } else {
                LOG.debug("Job queue currently empty");
                return;
            }
        }
    }
    
    
    @JmsListener(containerFactory = "dockerEventListenerFactory", destination = DockerEventsListener.DOCKER_EVENTS_QUEUE)
    @Override
    public void receiveDockerEvent(@Payload Event event) {
    	if (event.getType().equals(EventType.CONTAINER)) {
    		if (event.getAction().equals("die")) {
    			LOG.info("Container {} exited" + event.getActor().getId());
    			Map<String, String> containerAttributes = event.getActor().getAttributes();
    			if (containerAttributes.containsKey("jobId")) {
    				int exitCode = Integer.parseInt(containerAttributes.get("exitCode"));
    				jobExecutionController.terminateJob(containerAttributes.get("jobId"), exitCode);
    			}
    			
    		}
    	}
    }
}
