package com.cgi.eoss.fstep.persistence.dao;

import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.User;

import java.util.List;

public interface FstepServiceDao extends FstepEntityDao<FstepService> {
    List<FstepService> findByNameContainingIgnoreCase(String term);

    List<FstepService> findByOwner(User user);

    List<FstepService> findByStatus(FstepService.Status status);
}
