package com.cgi.eoss.fstep.persistence.dao;

import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.Job;
import com.cgi.eoss.fstep.model.User;

import java.util.List;

public interface JobDao extends FstepEntityDao<Job> {
    List<Job> findByOwner(User user);

    List<Job> findByConfig_Service(FstepService service);

    List<Job> findByOwnerAndConfig_Service(User user, FstepService service);
}
