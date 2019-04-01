package com.cgi.eoss.fstep.worker.worker;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.cgi.eoss.fstep.queues.service.FstepQueueService;
import com.cgi.eoss.fstep.worker.jobs.WorkerJob;

@Component
public class FstepWorkerUpdateManager implements JobUpdateListener{

	private FstepQueueService queueService;
	private String workerId;
	
	@Autowired
    public FstepWorkerUpdateManager(FstepQueueService queueService, @Qualifier("workerId") String workerId) {
        this.queueService = queueService;
        this.workerId = workerId;
    }

	@Override
    public void jobUpdate(WorkerJob workerJob, Object object) {
    	Map<String, Object> messageHeaders = new HashMap<>();
        messageHeaders.put("workerId", workerId);
        messageHeaders.put("jobId", workerJob.getIntJobId());
        queueService.sendObject(FstepQueueService.jobUpdatesQueueName, messageHeaders, object);
    }
}
