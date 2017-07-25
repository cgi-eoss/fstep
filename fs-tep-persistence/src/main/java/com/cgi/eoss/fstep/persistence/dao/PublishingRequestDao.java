package com.cgi.eoss.fstep.persistence.dao;

import com.cgi.eoss.fstep.model.PublishingRequest;
import com.cgi.eoss.fstep.model.User;

import java.util.List;

public interface PublishingRequestDao extends FstepEntityDao<PublishingRequest> {
    List<PublishingRequest> findByOwner(User user);
}
