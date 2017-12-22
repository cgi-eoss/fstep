package com.cgi.eoss.fstep.persistence.service;

import com.cgi.eoss.fstep.model.SystematicProcessing;
import com.cgi.eoss.fstep.model.SystematicProcessing.Status;
import java.util.List;

public interface SystematicProcessingDataService extends FstepEntityDataService<SystematicProcessing> {

    List<SystematicProcessing> findByStatus(Status s);


}
