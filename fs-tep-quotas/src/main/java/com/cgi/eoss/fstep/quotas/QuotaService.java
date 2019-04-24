package com.cgi.eoss.fstep.quotas;

import com.cgi.eoss.fstep.model.Quota;
import com.cgi.eoss.fstep.model.UsageType;

public interface QuotaService {

	Quota getQuota(UsageType usageType);


}