package com.cgi.eoss.fstep.persistence.service;

import com.cgi.eoss.fstep.model.ApiKey;
import com.cgi.eoss.fstep.model.User;

public interface ApiKeyDataService extends
        FstepEntityDataService<ApiKey> {

	ApiKey getByOwner(User user);
}
