package com.cgi.eoss.fstep.persistence.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cgi.eoss.fstep.model.QWalletTransaction;
import com.cgi.eoss.fstep.model.WalletTransaction;
import com.cgi.eoss.fstep.model.WalletTransaction.Type;
import com.cgi.eoss.fstep.persistence.dao.FstepEntityDao;
import com.cgi.eoss.fstep.persistence.dao.WalletTransactionDao;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.Expressions;

@Service
@Transactional(readOnly = true)
public class JpaWalletTransactionDataService extends AbstractJpaDataService<WalletTransaction> implements WalletTransactionDataService {
    
    private final WalletTransactionDao dao;

    @Autowired
    public JpaWalletTransactionDataService(WalletTransactionDao transactionDao) {
        this.dao = transactionDao;
    }

    @Override
    FstepEntityDao<WalletTransaction> getDao() {
        return dao;
    }

    @Override
    Predicate getUniquePredicate(WalletTransaction entity) {
    	BooleanBuilder builder = new BooleanBuilder();
    	builder.and(Expressions.asBoolean(entity.getId() != null));
    	builder.and(QWalletTransaction.walletTransaction.id.eq(entity.getId()));
    	return builder.getValue();
    }

	@Override
	public List<WalletTransaction> findByTypeAndAssociatedId(Type type, Long associatedId) {
		return dao.findByTypeAndAssociatedId(type, associatedId);
	}

}
