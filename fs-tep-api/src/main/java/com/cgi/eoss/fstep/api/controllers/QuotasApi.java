package com.cgi.eoss.fstep.api.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;

import com.cgi.eoss.fstep.model.Quota;
import com.cgi.eoss.fstep.model.UsageType;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.model.projections.ShortQuota;

@RepositoryRestResource(path = "quotas", itemResourceRel = "quota", collectionResourceRel = "quotas", excerptProjection = ShortQuota.class)
public interface QuotasApi extends BaseRepositoryApi<Quota>, PagingAndSortingRepository<Quota, Long>, QuotasApiCustom {

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    <S extends Quota> Iterable<S> save(Iterable<S> quotas);

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    <S extends Quota> S save(@P("quota") S quota);

    @Override
    @PostAuthorize("hasRole('ADMIN') or @fstepSecurityService.currentUser.equals(returnObject.owner)")
    Quota findOne(Long id);

    @Override
    @Query("select t from Quota t where t.owner=user")
    Page<Quota> findByOwner(@Param("owner") User user, Pageable pageable);

    @Override
    @Query("select t from Databasket t where not t.owner=user")
    Page<Quota> findByNotOwner(@Param("owner") User user, Pageable pageable);
    
    @Override
    @PostAuthorize("hasRole('ADMIN') or @fstepSecurityService.currentUser.equals(returnObject.owner)")
    @RestResource(path="findByUsageTypeAndOwner", rel="findByUsageTypeAndOwner")
    @Query("select t from Quota t where t.owner=:owner and t.usageType=:usageType)")
    Quota findByUsageTypeAndOwner(@Param("usageType") UsageType usageType, @Param("owner") User user);
    
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    void delete(Iterable<? extends Quota> quotas);

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    void delete(Quota quota);
}
