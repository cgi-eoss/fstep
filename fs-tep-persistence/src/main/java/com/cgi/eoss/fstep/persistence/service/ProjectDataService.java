package com.cgi.eoss.fstep.persistence.service;

import com.cgi.eoss.fstep.model.Databasket;
import com.cgi.eoss.fstep.model.JobConfig;
import com.cgi.eoss.fstep.model.Project;
import com.cgi.eoss.fstep.model.User;

import java.util.List;

public interface ProjectDataService extends
        FstepEntityDataService<Project>,
        SearchableDataService<Project> {
    Project getByNameAndOwner(String name, User user);

    List<Project> findByDatabasket(Databasket databasket);

    List<Project> findByJobConfig(JobConfig jobConfig);

    List<Project> findByOwner(User user);
}
