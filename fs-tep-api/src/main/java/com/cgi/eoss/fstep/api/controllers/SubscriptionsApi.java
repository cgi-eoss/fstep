package com.cgi.eoss.fstep.api.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;

import com.cgi.eoss.fstep.model.Subscription;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.model.projections.ShortSubscription;

@RepositoryRestResource(
        path = "subscriptions",
        itemResourceRel = "subscription",
        collectionResourceRel = "subscriptions",
        excerptProjection = ShortSubscription.class)
public interface SubscriptionsApi extends BaseRepositoryApi<Subscription>, SubscriptionsApiCustom, PagingAndSortingRepository<Subscription, Long> {

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or (#subscription.id == null) or hasPermission(#subscription, 'write')")
    <S extends Subscription> S save(@P("subscription") S subscription);
    
    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    void delete(@P("subscription") Subscription subscription);

    @Override
    @Query("select t from Subscription t where t.owner=user")
    Page<Subscription> findByOwner(@Param("owner") User user, Pageable pageable);

    @Override
    @Query("select t from Subscription t where not t.owner=user")
    Page<Subscription> findByNotOwner(@Param("owner") User user, Pageable pageable);

}
