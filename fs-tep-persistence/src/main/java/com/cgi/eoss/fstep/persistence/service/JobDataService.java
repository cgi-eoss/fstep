package com.cgi.eoss.fstep.persistence.service;

import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.Job;
import com.cgi.eoss.fstep.model.User;
import com.google.common.collect.Multimap;

import java.util.List;

public interface JobDataService extends
        FstepEntityDataService<Job> {

    List<Job> findByOwner(User user);

    List<Job> findByService(FstepService service);

    List<Job> findByOwnerAndService(User user, FstepService service);

    Job buildNew(String extId, String userId, String serviceId, String jobConfigLabel, Multimap<String, String> inputs);
    
    Job buildNew(String extId, String userId, String serviceId, String jobConfigLabel, Multimap<String, String> inputs, Job parentJob);
}
