package com.cgi.eoss.fstep.persistence.service;

import java.util.List;

import com.cgi.eoss.fstep.model.FstepServiceTemplate;
import com.cgi.eoss.fstep.model.User;

public interface ServiceTemplateDataService extends
        FstepEntityDataService<FstepServiceTemplate>,
        SearchableDataService<FstepServiceTemplate> {
    List<FstepServiceTemplate> findByOwner(User user);

    FstepServiceTemplate getByName(String serviceTemplateName);

}
