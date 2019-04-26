package com.cgi.eoss.fstep.persistence.service;

import com.cgi.eoss.fstep.model.CostingExpression;
import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.persistence.dao.CostingExpressionDao;
import com.cgi.eoss.fstep.persistence.dao.FstepEntityDao;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.cgi.eoss.fstep.model.QCostingExpression.costingExpression;

@Service
@Transactional(readOnly = true)
public class JpaCostingExpressionDataService extends AbstractJpaDataService<CostingExpression> implements CostingExpressionDataService {

    private final CostingExpressionDao costingExpressionDao;

    @Autowired
    public JpaCostingExpressionDataService(CostingExpressionDao costingExpressionDao) {
        this.costingExpressionDao = costingExpressionDao;
    }

    @Override
    FstepEntityDao<CostingExpression> getDao() {
        return costingExpressionDao;
    }

    @Override
    Predicate getUniquePredicate(CostingExpression entity) {
        return costingExpression.type.eq(entity.getType()).and(costingExpression.associatedId.eq(entity.getAssociatedId()));
    }

    @Override
    public CostingExpression getServiceCostingExpression(FstepService service) {
        return costingExpressionDao.findOne(
                costingExpression.type.eq(CostingExpression.Type.SERVICE)
                        .and(costingExpression.associatedId.eq(service.getId())));
    }

    @Override
    public CostingExpression getDownloadCostingExpression(FstepFile fstepFile) {
    	 if (fstepFile.getCollection() != null) {
    		 CostingExpression collectionCostingExpression = costingExpressionDao.findOne(
                     costingExpression.type.eq(CostingExpression.Type.COLLECTION)
                             .and(costingExpression.associatedId.eq(fstepFile.getCollection().getId())));
    		 if (collectionCostingExpression != null) {
    			 return collectionCostingExpression;
    		 }
                     
    	}
        if (fstepFile.getDataSource() != null) {
            return costingExpressionDao.findOne(
                    costingExpression.type.eq(CostingExpression.Type.DOWNLOAD)
                            .and(costingExpression.associatedId.eq(fstepFile.getDataSource().getId())));
        }
        return null;
    }

}
