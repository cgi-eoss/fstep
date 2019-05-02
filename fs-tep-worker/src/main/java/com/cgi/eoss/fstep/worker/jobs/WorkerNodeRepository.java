package com.cgi.eoss.fstep.worker.jobs;

import java.io.Serializable;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkerNodeRepository extends CrudRepository<WorkerNode, Serializable>{
    
   
}
