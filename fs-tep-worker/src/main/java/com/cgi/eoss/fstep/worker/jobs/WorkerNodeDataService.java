package com.cgi.eoss.fstep.worker.jobs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true, transactionManager = "workerJobsTransactionManager")
public class WorkerNodeDataService {
    
	@Autowired
	private WorkerNodeRepository workerNodeRepository;
	
	
	public WorkerNode findOne(String id) {
		return workerNodeRepository.findOne(id);
	}
	
	@Transactional(transactionManager = "workerJobsTransactionManager")
	public WorkerNode save(WorkerNode workerNode) {
		return workerNodeRepository.save(workerNode);
	}
	
	@Transactional(transactionManager = "workerJobsTransactionManager")
	public void delete(WorkerNode workerNode) {
		workerNodeRepository.delete(workerNode);
	}

}
