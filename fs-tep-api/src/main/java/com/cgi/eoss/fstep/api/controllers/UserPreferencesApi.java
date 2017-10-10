package com.cgi.eoss.fstep.api.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.model.UserPreference;
import com.cgi.eoss.fstep.model.projections.ShortUserPreference;

@RepositoryRestResource(path = "userPreferences", itemResourceRel = "userPreference",
        collectionResourceRel = "userPreferences", excerptProjection = ShortUserPreference.class)
public interface UserPreferencesApi extends BaseRepositoryApi<UserPreference>,
        PagingAndSortingRepository<UserPreference, Long>, UserPreferencesApiCustom {

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    <S extends UserPreference> Iterable<S> save(Iterable<S> userPreferences);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or (#userPreference.id == null) or hasPermission(#userPreference, 'write')")
    <S extends UserPreference> S save(@P("userPreference") S userPreference);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    void delete(Iterable<? extends UserPreference> userPreferences);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#userPreference, 'administration')")
    void delete(@P("userPreference") UserPreference userPreference);
   
    @Override
    @Query("select t from UserPreference t where t.owner=user and t.name=name and t.type=type")
    Page<UserPreference> findByOwner(@Param("owner") User user, @Param("name")String name, @Param("type")String type, Pageable pageable);

    @Override
    @Query("select t from UserPreference t where not t.owner=user")
    Page<UserPreference> findByNotOwner(@Param("owner") User user, Pageable pageable);

    @Override
    @RestResource(path = "findByName", rel = "findByName")
    @Query("select t from UserPreference t where t.name=name")
    Page<UserPreference> findByName(@Param("name") String name, Pageable pageable);
    
    @Override
    @RestResource(path = "findByType", rel = "findByType")
    @Query("select t from UserPreference t where t.type=type")
    Page<UserPreference> findByType(@Param("type") String type, Pageable pageable);

}
