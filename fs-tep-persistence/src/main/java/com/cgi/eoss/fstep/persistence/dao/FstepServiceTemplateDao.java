package com.cgi.eoss.fstep.persistence.dao;

import com.cgi.eoss.fstep.model.FstepServiceTemplate;
import com.cgi.eoss.fstep.model.User;

import java.util.List;

public interface FstepServiceTemplateDao extends FstepEntityDao<FstepServiceTemplate> {
    List<FstepServiceTemplate> findByNameContainingIgnoreCase(String term);

    List<FstepServiceTemplate> findByOwner(User user);

}
