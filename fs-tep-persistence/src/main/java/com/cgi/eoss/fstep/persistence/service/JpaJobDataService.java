package com.cgi.eoss.fstep.persistence.service;

import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.Job;
import com.cgi.eoss.fstep.model.Job.Status;
import com.cgi.eoss.fstep.model.JobConfig;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.persistence.dao.FstepEntityDao;
import com.cgi.eoss.fstep.persistence.dao.JobDao;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.cgi.eoss.fstep.model.QJob.job;

@Service
@Transactional(readOnly = true)
public class JpaJobDataService extends AbstractJpaDataService<Job> implements JobDataService {

    private final JobDao dao;

    private final JobConfigDataService jobConfigDataService;

    private final UserDataService userDataService;

    private final ServiceDataService serviceDataService;

    @Autowired
    public JpaJobDataService(JobDao jobDao, JobConfigDataService jobConfigDataService, UserDataService userDataService, ServiceDataService serviceDataService) {
        this.dao = jobDao;
        this.jobConfigDataService = jobConfigDataService;
        this.userDataService = userDataService;
        this.serviceDataService = serviceDataService;
    }

    @Override
    FstepEntityDao<Job> getDao() {
        return dao;
    }

    @Override
    Predicate getUniquePredicate(Job entity) {
        return job.extId.eq(entity.getExtId());
    }

    @Override
    public List<Job> findByOwner(User user) {
        return dao.findByOwner(user);
    }

    @Override
    public List<Job> findByService(FstepService service) {
        return dao.findByConfig_Service(service);
    }
    
    @Override
    public List<Job> findByStatusAndGuiUrlNotNull(Status status) {
        return dao.findByStatusAndGuiUrlNotNull(status);
    }

    @Override
    public List<Job> findByOwnerAndService(User user, FstepService service) {
        return dao.findByOwnerAndConfig_Service(user, service);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Job buildNew(String extId, String ownerId, String serviceId, String jobConfigLabel, Multimap<String, String> inputs, Job parentJob) {
        User owner = userDataService.getByName(ownerId);
        FstepService service = serviceDataService.getByName(serviceId);

        JobConfig config = new JobConfig(owner, service);
        config.setLabel(Strings.isNullOrEmpty(jobConfigLabel) ? null : jobConfigLabel);
        config.setInputs(inputs);
        config.setParent(parentJob);

        return dao.save(new Job(jobConfigDataService.save(config), extId, owner, parentJob));
    }
    
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Job buildNew(String extId, String ownerId, String serviceId, String jobConfigLabel, Multimap<String, String> inputs) {
    		return buildNew(extId, ownerId, serviceId, jobConfigLabel, inputs, null);
    }
    
    public Job refreshFull(Long id) {
        Job job = dao.findOne(id);
        job.getOwner().getGroups().size();
        job.getOutputFiles().size();
        job.getSubJobs().size();
        return job;
    }
    
    public Job refreshFull(Job job) {
        job = refresh(job);
        job.getOwner().getGroups().size();
        job.getOutputFiles().size();
        job.getSubJobs().size();
        job.getJobProcessings().size();        
        return job;
    }

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
    public Job updateParentJob(Job job) {
		Job parentJob = this.refreshFull(job.getParentJob());
		if (parentJob.getOutputs() == null) {
			parentJob.setOutputs(ArrayListMultimap.create());
		} 
		parentJob.getOutputs().putAll(job.getOutputs());
		parentJob.getOutputFiles().addAll(ImmutableSet.copyOf(job.getOutputFiles()));
        return save(parentJob);
		
	}
    
	@Override
	public Integer countByOwnerAndStatusIn(User user, List<Status> status) {
		return dao.countByOwnerAndStatusIn(user, status);
	}

	@Override
	public List<Job> findByOwnerAndParentFalseAndStartTimeBetween(User user, LocalDateTime startTime, LocalDateTime endTime) {
		return dao.findByOwnerAndParentFalseAndStartTimeBetween(user, startTime, endTime);
	}

}
