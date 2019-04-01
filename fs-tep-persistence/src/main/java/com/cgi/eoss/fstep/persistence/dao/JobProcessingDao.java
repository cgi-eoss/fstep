package com.cgi.eoss.fstep.persistence.dao;

import java.util.List;

import com.cgi.eoss.fstep.model.Job;
import com.cgi.eoss.fstep.model.JobProcessing;

public interface JobProcessingDao extends FstepEntityDao<JobProcessing> {

	public JobProcessing findFirstByJobOrderBySequenceNumDesc(Job job);
	public List<JobProcessing> findByJobOrderBySequenceNumAsc(Job job);

}
