package com.cgi.eoss.fstep.api.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.cgi.eoss.fstep.model.UserPreference;

public interface UserPreferencesApiCustom {

	Page<UserPreference> findByType(String type, Pageable pageable);

	Page<UserPreference> findByName(String name, Pageable pageable);
    
}
