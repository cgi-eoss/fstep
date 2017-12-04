package com.cgi.eoss.fstep.api.controllers;

import com.cgi.eoss.fstep.model.UserEndpoint;
import com.cgi.eoss.fstep.model.projections.ShortUserEndpoint;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;

@RepositoryRestResource(path = "userEndpoints", itemResourceRel = "userEndpoints",
        collectionResourceRel = "userEndpoints", excerptProjection = ShortUserEndpoint.class)
public interface UserEndpointsApi extends BaseRepositoryApi<UserEndpoint>,
        PagingAndSortingRepository<UserEndpoint, Long> {

    //Only admins can create/delete user endpoints
    @Override
    @PreAuthorize("hasAnyRole('ADMIN')")
    <S extends UserEndpoint> Iterable<S> save(Iterable<S> userEndpoints);
    
    @Override
    @PreAuthorize("hasAnyRole('ADMIN')")
    <S extends UserEndpoint> S save(@P("userEndpoint") S userEndpoint);

    @Override
    @PreAuthorize("hasAnyRole('ADMIN')")
    void delete(Iterable<? extends UserEndpoint> userEndpoint);

    @Override
    @PreAuthorize("hasAnyRole('ADMIN')")
    void delete(@P("userMount") UserEndpoint userEndpoint);

}
