package com.cgi.eoss.fstep.api.controllers;

import com.cgi.eoss.fstep.model.UserMount;
import com.cgi.eoss.fstep.model.projections.ShortUserMount;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;

@RepositoryRestResource(path = "userMounts", itemResourceRel = "userMounts",
        collectionResourceRel = "userMounts", excerptProjection = ShortUserMount.class)
public interface UserMountsApi extends BaseRepositoryApi<UserMount>,
        PagingAndSortingRepository<UserMount, Long> {

    //Only admins can create/delete user mounts
    @Override
    @PreAuthorize("hasAnyRole('ADMIN')")
    <S extends UserMount> Iterable<S> save(Iterable<S> userMounts);
    
    @Override
    @PreAuthorize("hasAnyRole('ADMIN')")
    <S extends UserMount> S save(@P("userMount") S userMount);

    @Override
    @PreAuthorize("hasAnyRole('ADMIN')")
    void delete(Iterable<? extends UserMount> userMount);

    @Override
    @PreAuthorize("hasAnyRole('ADMIN')")
    void delete(@P("userMount") UserMount userMount);

}
