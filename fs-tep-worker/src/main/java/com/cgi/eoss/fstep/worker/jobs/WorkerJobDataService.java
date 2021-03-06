package com.cgi.eoss.fstep.worker.jobs;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.cgi.eoss.fstep.worker.jobs.WorkerJob.Status;

@Component
@Transactional(readOnly = true, transactionManager = "workerJobsTransactionManager")
public class WorkerJobDataService {
    
	@Autowired
	private WorkerJobRepository workerJobRepository;
	
	@Transactional(transactionManager = "workerJobsTransactionManager")
	public WorkerJob save(WorkerJob job) {
		return workerJobRepository.save(job);
	}
	
	@Transactional(transactionManager = "workerJobsTransactionManager")
	public void delete(WorkerJob job) {
		workerJobRepository.delete(job);
	}

	@Transactional(transactionManager = "workerJobsTransactionManager")
	public boolean assignJobToNode(int maxJobsPerNode, WorkerJob workerJob, String workerNodeId) {
		if (workerJobRepository.countByWorkerNodeIdAndAssignedToWorkerNodeTrue(workerNodeId) < maxJobsPerNode) {
			workerJob.setWorkerNodeId(workerNodeId);
			workerJob.setAssignedToWorkerNode(true);
			workerJobRepository.save(workerJob);
			return true;
		}
		return false;
	}
	
	@Transactional(transactionManager = "workerJobsTransactionManager")
	public void releaseJobFromNode(WorkerJob workerJob) {
		workerJob.setAssignedToWorkerNode(false);
		workerJobRepository.save(workerJob);
	}
	
	@Transactional(transactionManager = "workerJobsTransactionManager")
	public void assignDeviceToJob(WorkerJob workerJob, String deviceId) {
		workerJob.setDeviceId(deviceId);
		workerJobRepository.save(workerJob);
	}
	
	public int countByWorkerNodeIdAndAssignedToWorkerNodeTrue(String workerNodeId) {
		return workerJobRepository.countByWorkerNodeIdAndAssignedToWorkerNodeTrue(workerNodeId);
	}
     
	public WorkerJob findByJobId(String id) {
		return workerJobRepository.findOne(id);
	}
	
	public List<WorkerJob> findByStatus(Status status) {
		return workerJobRepository.findByStatus(status);
	}
}
