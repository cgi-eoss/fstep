package com.cgi.eoss.fstep.api.controllers;

import java.time.OffsetDateTime;
import java.util.Collection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PostAuthorize;

import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.model.WalletTransaction;
import com.cgi.eoss.fstep.model.WalletTransaction.Type;
import com.cgi.eoss.fstep.model.projections.ShortWalletTransaction;

@RepositoryRestResource(path = "walletTransactions", itemResourceRel = "walletTransaction", collectionResourceRel = "walletTransactions", excerptProjection = ShortWalletTransaction.class)
public interface WalletTransactionsApi extends PagingAndSortingRepository<WalletTransaction, Long>, WalletTransactionsApiCustom {

    @Override
    @RestResource(exported = false)
    <S extends WalletTransaction> Iterable<S> save(Iterable<S> walletTransactions);

    @Override
    @RestResource(exported = false)
    <S extends WalletTransaction> S save(@P("walletTransaction") S walletTransaction);

    @Override
    @PostAuthorize("hasRole('ADMIN') or @fstepSecurityService.currentUser.equals(returnObject.owner)")
    WalletTransaction findOne(Long id);

    @Override
    @RestResource(exported = false)
    void delete(Iterable<? extends WalletTransaction> walletTransactions);

    @Override
    @RestResource(exported = false)
    void delete(WalletTransaction walletTransaction);

    @Override
    @RestResource(path="parametricFind", rel = "parametricFind")
    @Query("select t from WalletTransaction t where t.wallet.owner=:owner and t.type in (:type) and t.associatedId=:associatedId and t.transactionTime < :endDateTime and t.transactionTime > :startDateTime")
    Page<WalletTransaction> parametricFind(@Param("owner") User user, @Param("type") Collection<Type> types, @Param("associatedId") Long associatedId, @Param("startDateTime") @DateTimeFormat(iso = ISO.DATE_TIME) OffsetDateTime startDateTime, @Param("endDateTime") @DateTimeFormat(iso = ISO.DATE_TIME) OffsetDateTime endDateTime, Pageable pageable);

}
