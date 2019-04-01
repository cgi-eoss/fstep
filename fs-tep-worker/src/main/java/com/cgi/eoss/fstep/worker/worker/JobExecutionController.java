package com.cgi.eoss.fstep.worker.worker;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.cgi.eoss.fstep.rpc.GrpcUtil;
import com.cgi.eoss.fstep.rpc.Job;
import com.cgi.eoss.fstep.rpc.LocalWorker;
import com.cgi.eoss.fstep.rpc.worker.JobEvent;
import com.cgi.eoss.fstep.rpc.worker.JobEventType;
import com.cgi.eoss.fstep.rpc.worker.JobSpec;
import com.cgi.eoss.fstep.worker.jobs.WorkerJobDataService;
import com.cgi.eoss.fstep.worker.jobs.WorkerJob;
import com.cgi.eoss.fstep.worker.jobs.WorkerJob.Status;

import lombok.extern.log4j.Log4j2;


/**
 * <p>
 * Job execution controller
 * </p>
 */
@Service
@Log4j2
public class JobExecutionController {
    
    private static final long JOB_HEARTBEAT_DELAY = 300_000;
    
    private static final long JOB_TIMEOUT_DELAY = 60_000;

	private LocalWorker localWorker;
    
    private FstepWorkerNodeManager nodeManager;
    
    private WorkerJobDataService workerJobDataService;

	private JobUpdateListener jobUpdateListener;

    @Autowired
    public JobExecutionController(LocalWorker localWorker, FstepWorkerNodeManager nodeManager, WorkerJobDataService workerJobDataService, JobUpdateListener jobUpdateListener) {
        this.localWorker = localWorker;
        this.nodeManager = nodeManager;
        this.workerJobDataService = workerJobDataService;
        this.jobUpdateListener = jobUpdateListener;
    }

	public boolean hasCapacity() {
		return nodeManager.hasCapacity();
	}

	public boolean acceptJob(JobSpec jobSpec) {
        WorkerJob workerJob = new WorkerJob(jobSpec.getJob().getId(), jobSpec.getJob().getIntJobId());
        workerJob.setTimeoutMinutes(jobSpec.getTimeoutValue());
        workerJobDataService.save(workerJob);
        if (nodeManager.reserveNodeForJob(workerJob)) {
	        Thread t = new Thread(new JobLauncher(localWorker, workerJob, jobSpec, jobUpdateListener, nodeManager, workerJobDataService));
	        t.start();
	        return true;
        }
        return false;
	}
	
	public void terminateJob(String jobId, int exitCode) {
        WorkerJob workerJob = workerJobDataService.findByJobId(jobId);
        if (workerJob == null) {
        	LOG.error("Job " + jobId + " not found");
        	return;
        }
        workerJob.setExitCode(exitCode);
        workerJobDataService.save(workerJob);
        Thread t = new Thread(new JobTerminator(localWorker, workerJob, jobUpdateListener, nodeManager, workerJobDataService));
        t.start();
    }
	
	@Scheduled(fixedDelay = JOB_HEARTBEAT_DELAY)
	public void jobHeartbeat() {
		for (WorkerJob workerJob: workerJobDataService.findByStatus(Status.RUNNING)){
			OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Z"));
			jobUpdateListener.jobUpdate(workerJob, JobEvent.newBuilder().setJobEventType(JobEventType.HEARTBEAT).setTimestamp(GrpcUtil.timestampFromOffsetDateTime(now)).build());
		}
	}
	
	@Scheduled(fixedDelay = JOB_TIMEOUT_DELAY)
	public void timeoutJobs() {
		for (WorkerJob workerJob: workerJobDataService.findByStatus(Status.RUNNING)){
			if (workerJob.getTimeoutMinutes() > 0) {
				int timeout = workerJob.getTimeoutMinutes();
				long runTime = Duration.between(workerJob.getStart(), OffsetDateTime.now()).toMinutes();
				if (runTime > timeout) {
					localWorker.stopContainer(Job.newBuilder().setId(workerJob.getJobId()).build());
					return;
				}
			}
		}
		
	}

}
