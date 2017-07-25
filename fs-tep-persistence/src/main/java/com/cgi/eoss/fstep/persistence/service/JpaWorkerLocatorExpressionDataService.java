package com.cgi.eoss.fstep.persistence.service;

import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.WorkerLocatorExpression;
import com.cgi.eoss.fstep.persistence.dao.FstepEntityDao;
import com.cgi.eoss.fstep.persistence.dao.WorkerLocatorExpressionDao;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.cgi.eoss.fstep.model.QWorkerLocatorExpression.workerLocatorExpression;

@Service
@Transactional(readOnly = true)
public class JpaWorkerLocatorExpressionDataService extends AbstractJpaDataService<WorkerLocatorExpression> implements WorkerLocatorExpressionDataService {

    private final WorkerLocatorExpressionDao workerLocatorExpressionDao;

    @Autowired
    public JpaWorkerLocatorExpressionDataService(WorkerLocatorExpressionDao workerLocatorExpressionDao) {
        this.workerLocatorExpressionDao = workerLocatorExpressionDao;
    }

    @Override
    FstepEntityDao<WorkerLocatorExpression> getDao() {
        return workerLocatorExpressionDao;
    }

    @Override
    Predicate getUniquePredicate(WorkerLocatorExpression entity) {
        return workerLocatorExpression.service.eq(entity.getService());
    }

    @Override
    public WorkerLocatorExpression getByService(FstepService service) {
        return workerLocatorExpressionDao.findOneByService(service);
    }

}
