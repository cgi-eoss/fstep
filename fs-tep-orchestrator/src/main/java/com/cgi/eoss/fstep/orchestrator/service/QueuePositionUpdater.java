package com.cgi.eoss.fstep.orchestrator.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.cgi.eoss.fstep.model.Job;
import com.cgi.eoss.fstep.persistence.service.JobDataService;
import com.cgi.eoss.fstep.queues.service.BrowserClient;
import com.cgi.eoss.fstep.queues.service.FstepQueueService;
import com.cgi.eoss.fstep.queues.service.Message;

import lombok.extern.log4j.Log4j2;
/**
 * <p>
 * Gives an approximation of the job position in the execution queue
 * </p>
 */
@Service
@Log4j2
public class QueuePositionUpdater {
    
    
    private static final long QUEUE_CHECK_RATE_MS = 30_000;
	private final JobDataService jobDataService;
    private final FstepQueueService queueService;
    
    @Autowired
    public QueuePositionUpdater(JobDataService jobDataService, FstepQueueService queueService) {
        this.jobDataService = jobDataService;
        this.queueService = queueService;
    }

    @Scheduled(fixedDelay = QUEUE_CHECK_RATE_MS, initialDelay = 10000L)
    public void updateQueues() {
    	PositionUpdater pu = new PositionUpdater();
    	queueService.browse(FstepQueueService.jobExecutionQueueName, pu);
    }
    
    public class PositionUpdater implements BrowserClient{

    	private static final int BROWSE_LIMIT = 100;
    	
    	private int browsedCount = 0;
    	
    	public void handleMessage(Message m) {
    		browsedCount++;
    		Long jobId = Long.valueOf((String)m.getHeaders().get("jobId"));
    		Job job = jobDataService.getById(jobId);
			job.setQueuePosition(browsedCount);
			jobDataService.save(job);
			
    	}
    	
    	public boolean stopBrowsing() {
    		return browsedCount >= BROWSE_LIMIT;
    	}
    }
}
