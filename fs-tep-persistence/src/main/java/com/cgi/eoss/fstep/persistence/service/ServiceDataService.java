package com.cgi.eoss.fstep.persistence.service;

import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.User;

import java.util.List;

public interface ServiceDataService extends
        FstepEntityDataService<FstepService>,
        SearchableDataService<FstepService> {
    List<FstepService> findByOwner(User user);

    FstepService getByName(String serviceName);

    List<FstepService> findAllAvailable();

}
