package com.cgi.eoss.fstep.api.controllers;

import com.cgi.eoss.fstep.security.FstepSecurityService;
import com.cgi.eoss.fstep.model.QUser;
import com.cgi.eoss.fstep.model.QWallet;
import com.cgi.eoss.fstep.model.Wallet;
import com.cgi.eoss.fstep.persistence.dao.WalletDao;
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
public class WalletsApiImpl extends BaseRepositoryApiImpl<Wallet> {

    private final FstepSecurityService securityService;
    private final WalletDao dao;

    @Override
    NumberPath<Long> getIdPath() {
        return QWallet.wallet.id;
    }

    @Override
    QUser getOwnerPath() {
        return QWallet.wallet.owner;
    }

    @Override
    Class<Wallet> getEntityClass() {
        return Wallet.class;
    }

    @Override
    public Page<Wallet> findAll(Pageable pageable) {
        if (getSecurityService().isAdmin()) {
            return getDao().findAll(pageable);
        } else {
            BooleanExpression isOwned = QWallet.wallet.owner.eq(getSecurityService().getCurrentUser());
            return getDao().findAll(isOwned, pageable);
        }
    }

}
