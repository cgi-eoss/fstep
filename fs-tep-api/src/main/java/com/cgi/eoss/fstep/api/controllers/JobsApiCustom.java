package com.cgi.eoss.fstep.api.controllers;

import java.time.LocalDateTime;
import java.util.Collection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.cgi.eoss.fstep.model.Job;
import com.cgi.eoss.fstep.model.Job.Status;
import com.cgi.eoss.fstep.model.User;

public interface JobsApiCustom {

    Page<Job> findByFilterOnly(String filter, Collection<Status> statuses, Pageable pageable);

    Page<Job> findByFilterAndOwner(String filter, Collection<Status> statuses, User user, Pageable pageable);

    Page<Job> findByFilterAndNotOwner(String filter, Collection<Status> statuses, User user, Pageable pageable);
    
    Page<Job> findByFilterAndIsNotSubjob(String filter, Collection<Status> statuses, Pageable pageable);

    Page<Job> findByFilterAndIsNotSubjobAndOwner(String filter, Collection<Status> statuses, User user, Pageable pageable);

    Page<Job> findByFilterAndIsNotSubjobAndNotOwner(String filter, Collection<Status> statuses, User user, Pageable pageable);

    Page<Job> findByFilterAndParent(String filter, Collection<Status> statuses, Long parentId, Pageable pageable);

    Page<Job> findByFilterAndParentAndOwner(String filter, Collection<Status> statuses, Long parentId, User user, Pageable pageable);

    Page<Job> findByFilterAndParentAndNotOwner(String filter, Collection<Status> statuses, Long parentId, User user, Pageable pageable);
    
	Page<Job> parametricFind(String filter, Collection<Status> statuses, Long parentId, User user,
			User notOwner, String inputIdentifier, LocalDateTime startDateTime, LocalDateTime endDateTime,
			Pageable pageable);

}
