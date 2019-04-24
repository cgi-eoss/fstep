package com.cgi.eoss.fstep.api.controllers;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;

import com.cgi.eoss.fstep.model.SubscriptionPlan;
import com.cgi.eoss.fstep.model.projections.ShortSubscriptionPlan;

@RepositoryRestResource(
        path = "subscriptionPlans",
        itemResourceRel = "subscriptionPlan",
        collectionResourceRel = "subscriptionPlans",
        excerptProjection = ShortSubscriptionPlan.class)
public interface SubscriptionPlansApi extends PagingAndSortingRepository<SubscriptionPlan, Long> {

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    <S extends SubscriptionPlan> S save(@P("subscriptionPlan") S subscriptionPlan);
    
    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    void delete(@P("subscriptionPlan") SubscriptionPlan subscriptionPlan);

    

}
