package com.cgi.eoss.fstep.persistence.dao;

import com.cgi.eoss.fstep.model.ApiKey;
import com.cgi.eoss.fstep.model.User;

public interface ApiKeyDao extends FstepEntityDao<ApiKey> {
  
	public ApiKey getByOwner(User owner);
}
