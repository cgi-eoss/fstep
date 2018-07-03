package com.cgi.eoss.fstep.persistence.service;

import com.cgi.eoss.fstep.model.DefaultServiceTemplate;
import com.cgi.eoss.fstep.model.FstepService;

public interface DefaultServiceTemplateDataService extends
        FstepEntityDataService<DefaultServiceTemplate> {

	DefaultServiceTemplate getByServiceType(FstepService.Type serviceType);

}
