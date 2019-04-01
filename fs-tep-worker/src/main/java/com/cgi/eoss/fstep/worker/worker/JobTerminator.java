package com.cgi.eoss.fstep.worker.worker;

import com.cgi.eoss.fstep.rpc.GrpcUtil;
import com.cgi.eoss.fstep.rpc.Job;
import com.cgi.eoss.fstep.rpc.LocalWorker;
import com.cgi.eoss.fstep.rpc.worker.ContainerExit;
import com.cgi.eoss.fstep.worker.jobs.WorkerJob;
import com.cgi.eoss.fstep.worker.jobs.WorkerJobDataService;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class JobTerminator implements Runnable {

	private WorkerJob workerJob;
	private JobUpdateListener jobUpdateListener;
	private LocalWorker localWorker;
	private WorkerJobDataService workerJobDataService;
	
	public JobTerminator(LocalWorker localWorker, WorkerJob workerJob, JobUpdateListener jobUpdateListener, FstepWorkerNodeManager nodeManager, WorkerJobDataService workerJobDataService) {
		this.localWorker = localWorker;
		this.workerJob = workerJob;
		this.jobUpdateListener = jobUpdateListener;
		this.workerJobDataService = workerJobDataService;
	}

	@Override
	public void run() {
		terminateJob();
	}

	private void terminateJob(){
		int exitCode = workerJob.getExitCode();
		localWorker.cleanUp(Job.newBuilder().setId(workerJob.getJobId()).build());
		//Reload the job
		workerJob = workerJobDataService.findByJobId(workerJob.getJobId());
		jobUpdateListener.jobUpdate(workerJob, ContainerExit.newBuilder().setExitCode(exitCode).setOutputRootPath(workerJob.getOutputRootPath()).setTimestamp(GrpcUtil.timestampFromOffsetDateTime(workerJob.getEnd())).build());
	}
}