package com.cgi.eoss.fstep.persistence.dao;

import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.FstepServiceContextFile;

import java.util.List;

public interface FstepServiceContextFileDao extends FstepEntityDao<FstepServiceContextFile> {
    List<FstepServiceContextFile> findByService(FstepService service);
}
