package com.cgi.eoss.fstep.quotas;

import java.util.Optional;

import com.cgi.eoss.fstep.model.UsageType;
import com.cgi.eoss.fstep.model.User;

public interface UsageService {

	Optional<Long> getUsage(User user, UsageType usageType);


}