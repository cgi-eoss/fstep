package com.cgi.eoss.fstep.api.controllers;

import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ServicesApiCustom {
    Page<FstepService> findByFilterOnly(String filter, FstepService.Type serviceType, Pageable pageable);

    Page<FstepService> findByFilterAndOwner(String filter, User user, FstepService.Type serviceType, Pageable pageable);

    Page<FstepService> findByFilterAndNotOwner(String filter, User user, FstepService.Type serviceType, Pageable pageable);
}
