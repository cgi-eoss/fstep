package com.cgi.eoss.fstep.persistence.dao;

import com.cgi.eoss.fstep.model.DefaultServiceTemplate;
import com.cgi.eoss.fstep.model.FstepService;

public interface DefaultServiceTemplateDao extends FstepEntityDao<DefaultServiceTemplate> {
	
	public DefaultServiceTemplate getByServiceType(FstepService.Type type);

}
