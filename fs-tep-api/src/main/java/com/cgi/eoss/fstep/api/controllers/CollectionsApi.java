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
import com.cgi.eoss.fstep.model.Collection;
import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.model.projections.ShortCollection;

@RepositoryRestResource(
        path = "collections",
        itemResourceRel = "collection",
        collectionResourceRel = "collections",
        excerptProjection = ShortCollection.class)
public interface CollectionsApi extends BaseRepositoryApi<Collection>, CollectionsApiCustom, PagingAndSortingRepository<Collection, Long> {

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or (#collection.id == null) or hasPermission(#collection, 'write')")
    <S extends Collection> S save(@P("collection") S collection);
    
    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#collection, 'administration')")
    void delete(@P("collection") Collection collection);

    @Override
    @Query("select t from Collection t where t.owner=user")
    Page<Collection> findByOwner(@Param("owner") User user, Pageable pageable);

    @Override
    @Query("select t from Collection t where not t.owner=user")
    Page<Collection> findByNotOwner(@Param("owner") User user, Pageable pageable);

    @Override
    @RestResource(path="findByFilterOnly", rel="findByFilterOnly")
    @Query("select t from Collection t where t.name like %:filter% or t.description like %:filter%")
    Page<Collection> findByFilterOnly(@Param("filter") String filter, Pageable pageable);

    @Override
    @RestResource(path="findByFilterAndOwner", rel="findByFilterAndOwner")
    @Query("select t from Collection t where t.owner=:owner and (t.name like %:filter% or t.description like %:filter%)")
    Page<Collection> findByFilterAndOwner(@Param("filter") String filter, @Param("owner") User user, Pageable pageable);

    @Override
    @RestResource(path="findByFilterAndNotOwner", rel="findByFilterAndNotOwner")
    @Query("select t from Collection t where not t.owner=:owner and (t.name like %:filter% or t.description like %:filter%)")
    Page<Collection> findByFilterAndNotOwner(@Param("filter") String filter, @Param("owner") User user, Pageable pageable);

    @Override
    @RestResource(path="parametricFind", rel = "parametricFind")
    @Query("select t from Collection t where (t.id like %:filter% or t.name like %:filter%) and t.fileType=:fileType and not t.owner=:notOwner and t.owner=:owner")
    Page<Collection> parametricFind(@Param("filter") String filter, @Param("fileType") FstepFile.Type fileType, @Param("owner") User user,@Param("notOwner") User notOwner, Pageable pageable);

}
