package com.cgi.eoss.fstep.worker.jobs;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.cgi.eoss.fstep.worker.jobs.WorkerJob.Status;

@Repository
public interface WorkerJobRepository extends CrudRepository<WorkerJob, Serializable>{
    
   public int countByWorkerNodeIdAndAssignedToWorkerNodeTrue(String workerNodeId);
   
   public List<WorkerJob> findByStatus(Status status);
}
