package com.cgi.eoss.fstep.persistence.dao;

import com.cgi.eoss.fstep.model.Databasket;
import com.cgi.eoss.fstep.model.JobConfig;
import com.cgi.eoss.fstep.model.Project;
import com.cgi.eoss.fstep.model.User;

import java.util.List;

public interface ProjectDao extends FstepEntityDao<Project> {
    Project findOneByNameAndOwner(String name, User user);

    List<Project> findByNameContainingIgnoreCase(String term);

    List<Project> findByDatabasketsContaining(Databasket databasket);

    List<Project> findByJobConfigsContaining(JobConfig jobConfig);

    List<Project> findByOwner(User user);
}
