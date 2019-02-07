package com.cgi.eoss.fstep.api.controllers;

import com.cgi.eoss.fstep.model.Quota;
import com.cgi.eoss.fstep.model.UsageType;
import com.cgi.eoss.fstep.model.User;

public interface QuotasApiCustom {
    
	Quota findByUsageTypeAndOwner(UsageType usageType, User user);

}
