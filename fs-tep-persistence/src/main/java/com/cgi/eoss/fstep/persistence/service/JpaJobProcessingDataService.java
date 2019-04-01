package com.cgi.eoss.fstep.persistence.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cgi.eoss.fstep.model.Job;
import com.cgi.eoss.fstep.model.JobProcessing;
import com.cgi.eoss.fstep.persistence.dao.FstepEntityDao;
import com.cgi.eoss.fstep.persistence.dao.JobProcessingDao;
import com.querydsl.core.types.Predicate;

import static com.cgi.eoss.fstep.model.QJobProcessing.jobProcessing;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class JpaJobProcessingDataService extends AbstractJpaDataService<JobProcessing> implements JobProcessingDataService {

    private final JobProcessingDao dao;

    @Autowired
    public JpaJobProcessingDataService(JobProcessingDao jobProcessingDao) {
    	this.dao = jobProcessingDao;
    }

    @Override
    FstepEntityDao<JobProcessing> getDao() {
        return dao;
    }
	@Override
	Predicate getUniquePredicate(JobProcessing entity) {
		return jobProcessing.job.eq(entity.getJob()).and(jobProcessing.sequenceNum.eq(entity.getSequenceNum()));
	}

	@Override
	public JobProcessing findByJobAndMaxSequenceNum(Job job) {
		return dao.findFirstByJobOrderBySequenceNumDesc(job);
	}

	@Override
	public List<JobProcessing> findByJobOrderBySequenceNumAsc(Job job) {
		return dao.findByJobOrderBySequenceNumAsc(job);
	}

	@Override
	@Transactional
	public JobProcessing buildNew(Job job) {
		JobProcessing lastProcessing = findByJobAndMaxSequenceNum(job);
		JobProcessing newProcessing;
		if (lastProcessing == null) {
			newProcessing = new JobProcessing(job, 1);
		}
		else {
			newProcessing = new JobProcessing(job, lastProcessing.getSequenceNum() + 1);
		}
		return dao.save(newProcessing);
	}

}
