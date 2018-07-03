package com.cgi.eoss.fstep.api.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.FstepService.Type;
import com.cgi.eoss.fstep.model.FstepServiceTemplate;
import com.cgi.eoss.fstep.model.User;

public interface ServiceTemplatesApiCustom {
	
    Page<FstepServiceTemplate> findByFilterOnly(String filter, FstepService.Type serviceType, Pageable pageable);

    Page<FstepServiceTemplate> findByFilterAndOwner(String filter, User user, FstepService.Type serviceType, Pageable pageable);

    Page<FstepServiceTemplate> findByFilterAndNotOwner(String filter, User user, FstepService.Type serviceType, Pageable pageable);

	FstepServiceTemplate getDefaultByType(Type serviceType);

}
