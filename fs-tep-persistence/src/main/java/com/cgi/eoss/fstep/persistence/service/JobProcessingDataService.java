package com.cgi.eoss.fstep.persistence.service;

import java.util.List;

import com.cgi.eoss.fstep.model.Job;
import com.cgi.eoss.fstep.model.JobProcessing;

public interface JobProcessingDataService extends
        FstepEntityDataService<JobProcessing> {

	JobProcessing findByJobAndMaxSequenceNum(Job job);

	List<JobProcessing> findByJobOrderBySequenceNumAsc(Job job);
	
	JobProcessing buildNew(Job job);

}
