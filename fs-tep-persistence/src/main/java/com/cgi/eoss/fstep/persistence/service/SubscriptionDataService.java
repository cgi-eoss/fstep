package com.cgi.eoss.fstep.persistence.service;

import com.cgi.eoss.fstep.model.Subscription;
import com.cgi.eoss.fstep.model.Subscription.Status;
import com.cgi.eoss.fstep.model.UsageType;
import com.cgi.eoss.fstep.model.User;

public interface SubscriptionDataService extends FstepEntityDataService<Subscription> {
	
	Subscription findByOwnerAndSubscriptionPlanUsageTypeAndStatusIsNot(User owner, UsageType usageType, Status status);

  
}
