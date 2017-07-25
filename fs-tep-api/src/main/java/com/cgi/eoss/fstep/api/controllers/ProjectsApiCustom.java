package com.cgi.eoss.fstep.api.controllers;

import com.cgi.eoss.fstep.model.Project;
import com.cgi.eoss.fstep.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProjectsApiCustom {
    Page<Project> findByFilterOnly(String filter, Pageable pageable);

    Page<Project> findByFilterAndOwner(String filter, User user, Pageable pageable);

    Page<Project> findByFilterAndNotOwner(String filter, User user, Pageable pageable);
}
