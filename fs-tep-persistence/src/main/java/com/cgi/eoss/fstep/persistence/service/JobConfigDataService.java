package com.cgi.eoss.fstep.persistence.service;

import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.JobConfig;
import com.cgi.eoss.fstep.model.User;

import java.util.List;

public interface JobConfigDataService extends
        FstepEntityDataService<JobConfig> {

    List<JobConfig> findByOwner(User user);

    List<JobConfig> findByService(FstepService service);

    List<JobConfig> findByOwnerAndService(User user, FstepService service);

}
