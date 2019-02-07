package com.cgi.eoss.fstep.persistence.service;

import com.cgi.eoss.fstep.model.Quota;
import com.cgi.eoss.fstep.model.UsageType;
import com.cgi.eoss.fstep.model.User;

public interface QuotaDataService extends
        FstepEntityDataService<Quota> {

    Quota getByOwnerAndUsageType(User user, UsageType usageType);
	
}
