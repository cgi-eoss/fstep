package com.cgi.eoss.fstep.persistence.dao;

import java.util.List;
import com.cgi.eoss.fstep.model.SystematicProcessing;
import com.cgi.eoss.fstep.model.User;

public interface SystematicProcessingDao extends FstepEntityDao<SystematicProcessing> {

    List<SystematicProcessing> findByOwner(User user);


}
