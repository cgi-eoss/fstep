package com.cgi.eoss.fstep.persistence.dao;

import com.cgi.eoss.fstep.model.Quota;
import com.cgi.eoss.fstep.model.UsageType;
import com.cgi.eoss.fstep.model.User;

public interface QuotaDao extends FstepEntityDao<Quota> {
	Quota getByOwnerAndUsageType(User user, UsageType usageType);
}
