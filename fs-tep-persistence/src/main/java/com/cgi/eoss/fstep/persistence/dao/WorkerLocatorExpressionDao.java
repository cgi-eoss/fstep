package com.cgi.eoss.fstep.persistence.dao;

import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.WorkerLocatorExpression;

public interface WorkerLocatorExpressionDao extends FstepEntityDao<WorkerLocatorExpression> {
    WorkerLocatorExpression findOneByService(FstepService service);
}
