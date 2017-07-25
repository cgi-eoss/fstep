package com.cgi.eoss.fstep.persistence.service;

import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.FstepServiceContextFile;

import java.util.List;

public interface ServiceFileDataService extends
        FstepEntityDataService<FstepServiceContextFile> {
    List<FstepServiceContextFile> findByService(FstepService service);
}
