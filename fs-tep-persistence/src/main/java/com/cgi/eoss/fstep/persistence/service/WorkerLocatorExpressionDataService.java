package com.cgi.eoss.fstep.persistence.service;

import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.WorkerLocatorExpression;

public interface WorkerLocatorExpressionDataService extends
        FstepEntityDataService<WorkerLocatorExpression> {
    WorkerLocatorExpression getByService(FstepService service);
}
