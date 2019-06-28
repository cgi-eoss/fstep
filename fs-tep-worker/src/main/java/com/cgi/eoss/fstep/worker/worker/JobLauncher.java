package com.cgi.eoss.fstep.worker.worker;

import java.io.File;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.cgi.eoss.fstep.rpc.GrpcUtil;
import com.cgi.eoss.fstep.rpc.Job;
import com.cgi.eoss.fstep.rpc.LocalWorker;
import com.cgi.eoss.fstep.rpc.worker.DirectoryPermissions;
import com.cgi.eoss.fstep.rpc.worker.JobDockerConfig;
import com.cgi.eoss.fstep.rpc.worker.JobEnvironment;
import com.cgi.eoss.fstep.rpc.worker.JobError;
import com.cgi.eoss.fstep.rpc.worker.JobEvent;
import com.cgi.eoss.fstep.rpc.worker.JobEventType;
import com.cgi.eoss.fstep.rpc.worker.JobInputs;
import com.cgi.eoss.fstep.rpc.JobSpec;
import com.cgi.eoss.fstep.rpc.ResourceRequest;
import com.cgi.eoss.fstep.worker.jobs.WorkerJob;
import com.cgi.eoss.fstep.worker.jobs.WorkerJobDataService;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class JobLauncher implements Runnable {

	private WorkerJob workerJob;
	private JobUpdateListener jobUpdateListener;
	private LocalWorker localWorker;
	private FstepWorkerNodeManager nodeManager;
	private JobSpec jobSpec;
	private WorkerJobDataService workerJobDataService;
	
	public JobLauncher(LocalWorker localWorker, WorkerJob workerJob, JobSpec jobSpec, JobUpdateListener jobUpdateListener, FstepWorkerNodeManager nodeManager, WorkerJobDataService workerJobDataService) {
		this.localWorker = localWorker;
		this.workerJob = workerJob;
		this.jobSpec = jobSpec;
		this.jobUpdateListener = jobUpdateListener;
		this.nodeManager = nodeManager;
		this.workerJobDataService = workerJobDataService;
	}

	@Override
	public void run() {
		launchJob();
	}

	private void launchJob() {
		try {
			jobUpdateListener.jobUpdate(workerJob,
						JobEvent.newBuilder().setJobEventType(JobEventType.DATA_FETCHING_STARTED).setTimestamp(GrpcUtil.timestampFromOffsetDateTime(OffsetDateTime.now(ZoneId.of("Z")))).build());
			JobInputs jobInputs = JobInputs.newBuilder().setJob(jobSpec.getJob()).addAllInputs(jobSpec.getInputsList()).addAllPersistentFolders(jobSpec.getPersistentFoldersList())
						.build();
			JobEnvironment jobEnvironment = localWorker.prepareInputs(jobInputs);
			//Reload the job
			workerJob = workerJobDataService.findByJobId(workerJob.getJobId());
			jobUpdateListener
					.jobUpdate(workerJob, JobEvent.newBuilder().setJobEventType(JobEventType.DATA_FETCHING_COMPLETED).setTimestamp(GrpcUtil.timestampFromOffsetDateTime(OffsetDateTime.now(ZoneId.of("Z")))).build());
	
			List<String> ports = new ArrayList<>();
			ports.addAll(jobSpec.getExposedPortsList());
	
			List<String> binds = new ArrayList<>();
			binds.add("/data/dl:/data/dl:ro");// TODO Do not bind everything, just the required folder (can be derived)
			binds.add(jobEnvironment.getWorkingDir() + "/FSTEP-WPS-INPUT.properties:"
					+ "/home/worker/workDir/FSTEP-WPS-INPUT.properties:ro");
			binds.add(jobEnvironment.getInputDir() + ":" + "/home/worker/workDir/inDir:ro");
			binds.add(jobEnvironment.getOutputDir() + ":" + "/home/worker/workDir/outDir:rw");
			binds.add(jobEnvironment.getPersistentDir() + ":" + "/home/worker/workDir/persistentDir:rw");
            jobEnvironment.getAdditionalDirsList().stream().forEach(d -> 
            {
            	String permissions;
            	if (d.getPermissions().equals(DirectoryPermissions.RW)) {
            		permissions = "rw";
            	}
            	else if (d.getPermissions().equals(DirectoryPermissions.RO)) {
            		permissions = "ro";
            	}
            	else {
            		permissions = "ro";
            	}
            	binds.add(d.getPath() + ":" + d.getPath() + ":"  + permissions);
            });
			binds.addAll(jobSpec.getUserBindsList());
			Map<String, String> environmentVariables = jobSpec.getEnvironmentVariablesMap();
	
			if (jobSpec.hasResourceRequest()) {
				ResourceRequest resourceRequest = jobSpec.getResourceRequest();
				int requiredStorage = resourceRequest.getStorage();
				String procDir = generateRandomDirName("proc");
				File storageTempDir = new File("/dockerStorage", procDir);
				nodeManager.allocateStorageForJob(workerJob, requiredStorage,
						storageTempDir.getAbsolutePath());
				binds.add(storageTempDir.getAbsolutePath() + ":" + "/home/worker/procDir:rw");
			}

			JobDockerConfig request = JobDockerConfig.newBuilder().setJob(jobSpec.getJob())
					.setServiceName(jobSpec.getService().getName())
					.setDockerImage(jobSpec.getService().getDockerImageTag()).addAllBinds(binds).addAllPorts(ports)
					.putAllEnvironmentVariables(environmentVariables).build();
			localWorker.launchContainer(request);
			//Reload the job
			workerJob = workerJobDataService.findByJobId(workerJob.getJobId());
			jobUpdateListener.jobUpdate(workerJob, JobEvent.newBuilder().setJobEventType(JobEventType.PROCESSING_STARTED).setTimestamp(GrpcUtil.timestampFromOffsetDateTime(workerJob.getStart())).build());
		}catch (Exception e) {
			LOG.error("Error launching job", e);
			jobUpdateListener.jobUpdate(workerJob, JobError.newBuilder()
					.setErrorDescription(e.getMessage() != null ? e.getMessage() : "Unknown error").build());
			localWorker.cleanUp(Job.newBuilder().setId(workerJob.getJobId()).build());
		}

	}

	private String generateRandomDirName(String prefix) {
		long n = new Random().nextLong();
		n = (n == Long.MIN_VALUE) ? 0 : Math.abs(n);
		return prefix + Long.toString(n);
	}

}