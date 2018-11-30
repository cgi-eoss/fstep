package com.cgi.eoss.fstep.api.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.security.access.prepost.PostAuthorize;

import com.cgi.eoss.fstep.model.SystematicProcessing;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.model.projections.ShortSystematicProcessing;

@RepositoryRestResource(path = "systematicProcessings", itemResourceRel = "systematicProcessing", collectionResourceRel = "systematicProcessings", excerptProjection = ShortSystematicProcessing.class)
public interface SystematicProcessingsApi extends BaseRepositoryApi<SystematicProcessing>, PagingAndSortingRepository<SystematicProcessing, Long> {

    @Override
    @PostAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(returnObject, 'read')")
    SystematicProcessing findOne(Long id);

    // Users cannot create systematic processing instances via the API; these are created by launching a job in systematic mode 
    @Override
    @RestResource(exported = false)
    <S extends SystematicProcessing> Iterable<S> save(Iterable<S> systematicProcessings);

    // Users cannot create processing instances via the API; these are created by launching a job in systematic mode  
    @Override
    @RestResource(exported = false)
    <S extends SystematicProcessing> S save(S job);

   

    @Override
    @Query("select t from SystematicProcessing t where t.owner=user")
    Page<SystematicProcessing> findByOwner(@Param("owner") User user, Pageable pageable);

    @Override
    @Query("select t from SystematicProcessing t where not t.owner=user")
    Page<SystematicProcessing> findByNotOwner(@Param("owner") User user, Pageable pageable);

}
