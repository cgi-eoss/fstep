package com.cgi.eoss.fstep.worker.worker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.cgi.eoss.fstep.queues.service.FstepQueueService;
import com.cgi.eoss.fstep.rpc.worker.ContainerExit;
import com.cgi.eoss.fstep.rpc.worker.ContainerExitCode;
import com.cgi.eoss.fstep.rpc.worker.ExitParams;
import com.cgi.eoss.fstep.rpc.worker.ExitWithTimeoutParams;
import com.cgi.eoss.fstep.rpc.worker.FstepWorkerGrpc.FstepWorkerBlockingStub;
import com.cgi.eoss.fstep.rpc.worker.JobDockerConfig;
import com.cgi.eoss.fstep.rpc.worker.JobEnvironment;
import com.cgi.eoss.fstep.rpc.worker.JobError;
import com.cgi.eoss.fstep.rpc.worker.JobEvent;
import com.cgi.eoss.fstep.rpc.worker.JobEventType;
import com.cgi.eoss.fstep.rpc.worker.JobInputs;
import com.cgi.eoss.fstep.rpc.worker.JobSpec;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

/**
 * <p>
 * Service for executing FS-TEP (WPS) services inside Docker containers.
 * </p>
 */
@Log4j2
@Service
public class FstepWorkerDispatcher {
	
	private FstepQueueService queueService;
	
	private WorkerLocator workerLocator;
	
	private long capacityMissingInterval;
	
	private long queueEmptyInterval;
	
	//TODO move this to configuration
	private static final long QUEUE_SCHEDULER_INTERVAL_MS = 30L * 1000L;
	
	//TODO move this to configuration
	private static final long SCALE_UP_THRESHOLD_MS = 10 * 60L * 1000L;
	
	//TODO move this to configuration
	private static final long SCALE_DOWN_THRESHOLD_MS = 10 * 60L * 1000L;
	
	private FstepWorker worker;
	
	private String workerId;

	
	@Autowired
	public FstepWorkerDispatcher(FstepQueueService queueService, WorkerLocator workerLocator, FstepWorker worker, String workerId) {
		this.queueService = queueService;
		this.workerLocator = workerLocator;
		this.worker = worker;
		this.workerId = workerId;
		capacityMissingInterval =0L;
		
	}

	
	@Scheduled(fixedRate= QUEUE_SCHEDULER_INTERVAL_MS,initialDelay = 10000L)
	public void getNewJobs(){
		while (worker.hasCapacity()) {
			capacityMissingInterval = 0L;
			JobSpec nextJobSpec = (JobSpec)  queueService.receiveObjectWithTimeout(FstepQueueService.jobQueueName, 100);
			if (nextJobSpec != null) {
				queueEmptyInterval = 0L;
				worker.reserveNodeForJob(nextJobSpec.getJob());
				Thread t = new Thread(new JobExecutor(worker, nextJobSpec, queueService));
	            t.start();
			}
			else {
				LOG.debug("Job queue currently empty");
				queueEmptyInterval+= QUEUE_SCHEDULER_INTERVAL_MS;
				if (queueEmptyInterval > SCALE_DOWN_THRESHOLD_MS) {
					LOG.info("Requesting scale down as queue has been empty for {} ms", SCALE_DOWN_THRESHOLD_MS);
					queueEmptyInterval=0;
					worker.scaleDown();
				}
				return;
			}
		}
		LOG.info("Worker does not have capacity, waiting scale up");
		capacityMissingInterval += QUEUE_SCHEDULER_INTERVAL_MS;
		if (capacityMissingInterval > SCALE_UP_THRESHOLD_MS) {
		    LOG.info("Requesting scale up as no worker node has been found for {} ms", SCALE_UP_THRESHOLD_MS);
					worker.scaleUp();
		}
	}
	
	public interface JobUpdateListener {

		void jobUpdate(Object object);
		
	}
	
	@Data
	public class JobExecutor implements Runnable, JobUpdateListener{

		Map<String, Object> messageHeaders;
		private final FstepWorker worker;
		private final JobSpec jobSpec;
		private final FstepQueueService queueService;
		
		
		@Override
		public void run() {
			messageHeaders = new HashMap<>();
			messageHeaders.put("workerId", workerId);
			messageHeaders.put("jobId", jobSpec.getJob().getIntJobId());
			executeJob(jobSpec, this);
			
		}


		@Override
		public void jobUpdate(Object object) {
			queueService.sendObject(FstepQueueService.jobUpdatesQueueName, messageHeaders, object);
		}
		
	}
	
	
	// Entry point after Job is dequeued
	private void executeJob(JobSpec jobSpec, JobUpdateListener jobUpdateListener) {
		
		try {
			FstepWorkerBlockingStub worker = workerLocator.getWorkerById(workerId);
			jobUpdateListener.jobUpdate(JobEvent.newBuilder().setJobEventType(JobEventType.DATA_FETCHING_STARTED).build());
			JobInputs jobInputs = JobInputs.newBuilder().setJob(jobSpec.getJob()).addAllInputs(jobSpec.getInputsList()).build();
			JobEnvironment jobEnvironment = worker.prepareInputs(jobInputs);
			jobUpdateListener.jobUpdate(JobEvent.newBuilder().setJobEventType(JobEventType.DATA_FETCHING_COMPLETED).build());
			
			List<String> ports = new ArrayList<String>();
			ports.addAll(jobSpec.getExposedPortsList());
			
			List<String> binds = new ArrayList<String>();
			binds.add("/data:/data:ro");// TODO Do not bind everything, just the required folder (can be derived)
			binds.add(jobEnvironment.getWorkingDir() + "/FSTEP-WPS-INPUT.properties:"
					+ "/home/worker/workDir/FSTEP-WPS-INPUT.properties:ro");
			binds.add(jobEnvironment.getInputDir() + ":" + "/home/worker/workDir/inDir:ro");
			binds.add(jobEnvironment.getOutputDir() + ":" + "/home/worker/workDir/outDir:rw");
			
			JobDockerConfig request = JobDockerConfig.newBuilder().setJob(jobSpec.getJob())
			.setServiceName(jobSpec.getService().getName())
			.setDockerImage(jobSpec.getService().getDockerImageTag())
			.addAllBinds(binds)
			.addAllPorts(ports)
			.build();
			worker.launchContainer(request);
			jobUpdateListener.jobUpdate(JobEvent.newBuilder().setJobEventType(JobEventType.PROCESSING_STARTED).build());
            int exitCode;
			if (jobSpec.getHasTimeout()) {
				ExitWithTimeoutParams exitRequest = ExitWithTimeoutParams.newBuilder().setJob(jobSpec.getJob()).setTimeout(jobSpec.getTimeoutValue()).build();
				ContainerExitCode containerExitCode = worker.waitForContainerExitWithTimeout(exitRequest);
				exitCode = containerExitCode.getExitCode();
			} else {
				ExitParams exitRequest = ExitParams.newBuilder().setJob(jobSpec.getJob()).build();
				ContainerExitCode containerExitCode = worker.waitForContainerExit(exitRequest);
				exitCode = containerExitCode.getExitCode();
			}
			jobUpdateListener.jobUpdate(ContainerExit.newBuilder().setExitCode(exitCode).setJobEnvironment(jobEnvironment).build());
		} catch (Exception e) {
			jobUpdateListener.jobUpdate(JobError.newBuilder().setErrorDescription(e.getMessage()).build());
			
		}
	}

}
