package com.cgi.eoss.fstep.api.controllers;

import com.cgi.eoss.fstep.model.Job;
import com.cgi.eoss.fstep.model.Job.Status;
import com.cgi.eoss.fstep.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;

public interface JobsApiCustom {

    Page<Job> findByFilterOnly(String filter, Collection<Status> statuses, Pageable pageable);

    Page<Job> findByFilterAndOwner(String filter, Collection<Status> statuses, User user, Pageable pageable);

    Page<Job> findByFilterAndNotOwner(String filter, Collection<Status> statuses, User user, Pageable pageable);
}
