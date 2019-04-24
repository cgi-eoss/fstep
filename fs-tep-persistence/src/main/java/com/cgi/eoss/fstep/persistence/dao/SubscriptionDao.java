package com.cgi.eoss.fstep.persistence.dao;

import com.cgi.eoss.fstep.model.Subscription;
import com.cgi.eoss.fstep.model.Subscription.Status;
import com.cgi.eoss.fstep.model.UsageType;
import com.cgi.eoss.fstep.model.User;

public interface SubscriptionDao extends FstepEntityDao<Subscription> {
	
	Subscription findByOwnerAndSubscriptionPlanUsageTypeAndStatusIsNot(User owner, UsageType usageType, Status status);

 

}
