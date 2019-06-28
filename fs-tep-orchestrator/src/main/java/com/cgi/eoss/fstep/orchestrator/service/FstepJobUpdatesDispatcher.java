package com.cgi.eoss.fstep.orchestrator.service;

import com.cgi.eoss.fstep.model.Job;
import com.cgi.eoss.fstep.persistence.service.JobDataService;
import com.cgi.eoss.fstep.queues.service.FstepQueueService;
import com.cgi.eoss.fstep.rpc.worker.ContainerExit;
import com.cgi.eoss.fstep.rpc.worker.JobError;
import com.cgi.eoss.fstep.rpc.worker.JobEvent;
import com.cgi.eoss.fstep.rpc.worker.JobEventType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import java.io.Serializable;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;

@Component
public class FstepJobUpdatesDispatcher {

    private final JobDataService jobDataService;
    private FstepJobUpdatesManager fstepJobUpdatesManager;


    @Autowired
    public FstepJobUpdatesDispatcher(JobDataService jobDataService, FstepJobUpdatesManager fstepJobUpdatesManager) {
        this.jobDataService = jobDataService;
        this.fstepJobUpdatesManager = fstepJobUpdatesManager;

    }

    @JmsListener(destination = FstepQueueService.jobUpdatesQueueName)
    public void receiveJobUpdateMessage(@Payload ObjectMessage objectMessage, @Header("workerId") String workerId,
            @Header("jobId") String internalJobId) {
        try {
        	Serializable update = objectMessage.getObject();
            dispatchJobUpdate(update, workerId, internalJobId);
        } catch (JMSException e) {
        	Job job = jobDataService.refreshFull(Long.parseLong(internalJobId));
            fstepJobUpdatesManager.onJobError(job, e);
        }

    }
    
    public void dispatchJobUpdate(Object update, String workerId, String internalJobId) {
        Job job = jobDataService.refreshFull(Long.parseLong(internalJobId));
        if (update instanceof JobEvent) {
            JobEvent jobEvent = (JobEvent) update;
            JobEventType jobEventType = jobEvent.getJobEventType();
            if (jobEventType == JobEventType.DATA_FETCHING_STARTED) {
                fstepJobUpdatesManager.onJobDataFetchingStarted(job, workerId);
            } else if (jobEventType == JobEventType.DATA_FETCHING_COMPLETED) {
            	fstepJobUpdatesManager.onJobDataFetchingCompleted(job);
            } else if (jobEventType == JobEventType.PROCESSING_STARTED) {
            	fstepJobUpdatesManager.onJobProcessingStarted(job, workerId, jobEvent.getTimestamp());
            }
            else if (jobEventType == JobEventType.HEARTBEAT) {
            	fstepJobUpdatesManager.onJobHeartbeat(job, jobEvent.getTimestamp());
            }
        } else if (update instanceof JobError) {
            JobError jobError = (JobError) update;
            fstepJobUpdatesManager.onJobError(job, jobError.getErrorDescription());
        } else if (update instanceof ContainerExit) {
            ContainerExit containerExit = (ContainerExit) update;
            try {
            	fstepJobUpdatesManager.onContainerExit(job, workerId, containerExit.getOutputRootPath(),
                        containerExit.getExitCode(), containerExit.getTimestamp());
            } catch (Exception e) {
            	fstepJobUpdatesManager.onJobError(job, e);
            }
        }
    }
}
