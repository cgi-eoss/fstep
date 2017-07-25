package com.cgi.eoss.fstep.api.controllers;

import com.cgi.eoss.fstep.model.Group;
import com.cgi.eoss.fstep.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GroupsApiCustom {
    Page<Group> findByFilterOnly(String filter, Pageable pageable);

    Page<Group> findByFilterAndOwner(String filter, User user, Pageable pageable);

    Page<Group> findByFilterAndNotOwner(String filter, User user, Pageable pageable);
}
