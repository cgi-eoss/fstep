package com.cgi.eoss.fstep.api.controllers;

import com.cgi.eoss.fstep.security.FstepSecurityService;
import com.cgi.eoss.fstep.model.QUser;
import com.cgi.eoss.fstep.model.QWalletTransaction;
import com.cgi.eoss.fstep.model.WalletTransaction;
import com.cgi.eoss.fstep.persistence.dao.WalletTransactionDao;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Getter
@Component
public class WalletTransactionsApiImpl extends BaseRepositoryApiImpl<WalletTransaction> {

    private final FstepSecurityService securityService;
    private final WalletTransactionDao dao;

    @Override
    NumberPath<Long> getIdPath() {
        return QWalletTransaction.walletTransaction.id;
    }

    @Override
    QUser getOwnerPath() {
        return QWalletTransaction.walletTransaction.wallet.owner;
    }

    @Override
    Class<WalletTransaction> getEntityClass() {
        return WalletTransaction.class;
    }

    @Override
    public Page<WalletTransaction> findAll(Pageable pageable) {
        if (getSecurityService().isAdmin()) {
            return getDao().findAll(pageable);
        } else {
            BooleanExpression isOwned = QWalletTransaction.walletTransaction.wallet.owner.eq(getSecurityService().getCurrentUser());
            return getDao().findAll(isOwned, pageable);
        }
    }

}
