package com.cgi.eoss.fstep.orchestrator.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.cgi.eoss.fstep.model.Job;
import com.cgi.eoss.fstep.model.Job.Status;
import com.cgi.eoss.fstep.model.Quota;
import com.cgi.eoss.fstep.model.UsageType;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.persistence.service.JobDataService;
import com.cgi.eoss.fstep.persistence.service.QuotaDataService;
import com.cgi.eoss.fstep.queues.service.BrowserClient;
import com.cgi.eoss.fstep.queues.service.FstepQueueService;
import com.cgi.eoss.fstep.queues.service.Message;
import com.google.common.collect.ImmutableList;

import lombok.extern.log4j.Log4j2;
/**
 * <p>
 * Manages quota, execution and waiting queue
 * </p>
 */
@Service
@Log4j2
public class QueueScheduler {
    
    
    private static final long QUEUE_UPDATE_RATE_MS = 5_000;
	private final JobDataService jobDataService;
    private final FstepQueueService queueService;
    private final QuotaDataService quotaService;
    
    @Autowired
    public QueueScheduler(JobDataService jobDataService, FstepQueueService queueService, QuotaDataService quotaService) {
        this.jobDataService = jobDataService;
        this.queueService = queueService;
        this.quotaService = quotaService;
    }

    @Scheduled(fixedDelay = QUEUE_UPDATE_RATE_MS, initialDelay = 10000L)
    public void updateQueues() {
    	SchedulePlanner s = new SchedulePlanner();
    	queueService.browse(FstepQueueService.jobPendingQueueName, s);
    	for (Job job: s.getSelectedJobs()) {
    		moveJobToExecutionQueue(job);
    	}
    }
    
    public class SchedulePlanner implements BrowserClient{

    	private static final int BROWSE_LIMIT = 500;
    	
    	private static final int SELECT_LIMIT = 100;
    	
    	private int browsedCount = 0;
    	
    	private int selectedCount = 0;

    	private Map<User, Integer> selectedJobsByOwner = new HashMap<>();
    	
    	private Map<User, Integer> userRunningJobsCache = new HashMap<>();
    	
    	private Map<User, Long> userMaxJobQuotaCache = new HashMap<>();

    	private List<Job> selectedJobs = new ArrayList<>();
    	
    	public void handleMessage(Message m) {
    		Long jobId = Long.valueOf((String)m.getHeaders().get("jobId"));
    		Job job = jobDataService.getById(jobId);
			if (isSchedulable(job)) {
				selectedJobs.add(job);
				selectedJobsByOwner.put(job.getOwner(), selectedJobsByOwner.getOrDefault(job.getOwner(), 0) + 1);
				selectedCount++;
			}
			browsedCount++;
    	}
    	
    	private boolean isSchedulable(Job job) {
    		if (userMaxJobQuotaCache.get(job.getOwner()) == null) {
    			//Retrieve user quota
    			Quota userQuota = quotaService.getByOwnerAndUsageType(job.getOwner(), UsageType.MAX_RUNNABLE_JOBS);
    			Long userQuotaValue;
    			if(userQuota != null) {
    				userQuotaValue = userQuota.getValue();
    			}
    			else {
    				userQuotaValue = UsageType.MAX_RUNNABLE_JOBS.getDefaultValue();
    			}
    			userMaxJobQuotaCache.put(job.getOwner(), userQuotaValue);
    		}
    		Long userMaxRunnableJobs = userMaxJobQuotaCache.get(job.getOwner());
    		if (userRunningJobsCache.get(job.getOwner()) == null) {
    			//Retrieve user running jobs
    			userRunningJobsCache.put(job.getOwner(), jobDataService.countByOwnerAndStatusIn(job.getOwner(), ImmutableList.of(Status.RUNNING, Status.WAITING)));
    		}
			Integer userRunningJobs = userRunningJobsCache.get(job.getOwner());
			Integer userSelectedJobs = selectedJobsByOwner.getOrDefault(job.getOwner(), 0);
			if (userMaxRunnableJobs > userRunningJobs + userSelectedJobs) {
				return true;
			}
			else {
				return false;
			}
    	}
    	
    	public boolean stopBrowsing() {
    		return selectedCount >= SELECT_LIMIT || browsedCount >= BROWSE_LIMIT;
    	}
    	
    	public List<Job> getSelectedJobs(){
    		return selectedJobs;
    	}
    }

	
	
	private void moveJobToExecutionQueue(Job job) {
		LOG.info("Moving job {} to execution queue", String.valueOf(job.getId()));
		Message jobMessage = queueService.receiveSelected(FstepQueueService.jobPendingQueueName, "jobId = '" + job.getId() + "'");
    	queueService.send(FstepQueueService.jobExecutionQueueName, jobMessage);
    	job.setStatus(Status.WAITING);
    	jobDataService.save(job);
	}
	
}
