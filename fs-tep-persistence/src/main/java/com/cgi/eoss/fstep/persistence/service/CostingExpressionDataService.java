package com.cgi.eoss.fstep.persistence.service;

import com.cgi.eoss.fstep.model.CostingExpression;
import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.FstepService;

public interface CostingExpressionDataService extends
        FstepEntityDataService<CostingExpression> {
    CostingExpression getServiceCostingExpression(FstepService service);
    CostingExpression getDownloadCostingExpression(FstepFile fstepFile);
}
