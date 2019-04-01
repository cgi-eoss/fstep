package com.cgi.eoss.fstep.api.controllers;

import java.time.OffsetDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.cgi.eoss.fstep.model.QUser;
import com.cgi.eoss.fstep.model.QWalletTransaction;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.model.WalletTransaction;
import com.cgi.eoss.fstep.model.WalletTransaction.Type;
import com.cgi.eoss.fstep.persistence.dao.WalletTransactionDao;
import com.cgi.eoss.fstep.security.FstepSecurityService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberPath;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Getter
@Component
public class WalletTransactionsApiImpl extends BaseRepositoryApiImpl<WalletTransaction> implements WalletTransactionsApiCustom{

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

	@Override
	public Page<WalletTransaction> parametricFind(User owner, Type type, Long associatedId, OffsetDateTime startDateTime,
			OffsetDateTime endDateTime, Pageable pageable) {
		Predicate p = buildPredicate(owner, type, associatedId, startDateTime, endDateTime);
		return getFilteredResults(p, pageable);
	}

	private Predicate buildPredicate(User owner, Type type, Long associatedId, OffsetDateTime startDateTime,
			OffsetDateTime endDateTime) {
		BooleanBuilder builder = new BooleanBuilder(Expressions.asBoolean(true).isTrue());
		if (type != null) {
			builder.and(QWalletTransaction.walletTransaction.type.eq(type));
		}
		if (owner != null) {
			builder.and(getOwnerPath().eq(owner));
		}
		if (associatedId != null) {
			builder.and(QWalletTransaction.walletTransaction.associatedId.eq(associatedId));
		}
		if (startDateTime != null && endDateTime != null) {
			builder.and(QWalletTransaction.walletTransaction.transactionTime.between(startDateTime.toLocalDateTime(), endDateTime.toLocalDateTime()));
		}
		else if (startDateTime != null) {
			builder.and(QWalletTransaction.walletTransaction.transactionTime.after(startDateTime.toLocalDateTime()));
		}
		else if (endDateTime != null){
			builder.and(QWalletTransaction.walletTransaction.transactionTime.before(endDateTime.toLocalDateTime()));
		}
		return builder.getValue();
	}
	

}
