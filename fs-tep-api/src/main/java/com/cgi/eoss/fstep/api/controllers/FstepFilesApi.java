package com.cgi.eoss.fstep.api.controllers;

import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.model.projections.ShortFstepFile;
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

import java.net.URI;
import java.util.UUID;

@RepositoryRestResource(path = "fstepFiles", itemResourceRel = "fstepFile", collectionResourceRel = "fstepFiles", excerptProjection = ShortFstepFile.class)
public interface FstepFilesApi extends BaseRepositoryApi<FstepFile>, FstepFilesApiCustom, PagingAndSortingRepository<FstepFile, Long> {

    @Override
    @PostAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(returnObject, 'read')")
    FstepFile findOne(Long id);

    @Override
    @RestResource(exported = false)
    <S extends FstepFile> Iterable<S> save(Iterable<S> fstepFiles);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or (#fstepFile.id != null and hasPermission(#fstepFile, 'write'))")
    <S extends FstepFile> S save(@P("fstepFile") S fstepFile);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    void delete(Iterable<? extends FstepFile> fstepFiles);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#fstepFile, 'administration')")
    void delete(@P("fstepFile") FstepFile fstepFile);

    @Override
    @Query("select f from FstepFile f where f.type=type")
    Page<FstepFile> findByType(@Param("type") FstepFile.Type type, Pageable pageable);

    @PostAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(returnObject, 'read')")
    FstepFile findOneByUri(@Param("uri") URI uri);

    @PostAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(returnObject, 'read')")
    FstepFile findOneByRestoId(@Param("uuid") UUID uuid);

    @Override
    @Query("select t from FstepFile t where t.owner=user")
    Page<FstepFile> findByOwner(@Param("owner") User user, Pageable pageable);

    @Override
    @Query("select t from FstepFile t where not t.owner=user")
    Page<FstepFile> findByNotOwner(@Param("owner") User user, Pageable pageable);

    @Override
    @RestResource(path = "findByFilterOnly", rel = "findByFilterOnly")
    @Query("select t from FstepFile t where t.filename like %:filter% and t.type=:type")
    Page<FstepFile> findByFilterOnly(@Param("filter") String filter, @Param("type") FstepFile.Type type, Pageable pageable);

    @Override
    @RestResource(path = "findByFilterAndOwner", rel = "findByFilterAndOwner")
    @Query("select t from FstepFile t where t.owner=:owner and t.filename like %:filter% and t.type=:type")
    Page<FstepFile> findByFilterAndOwner(@Param("filter") String filter, @Param("type") FstepFile.Type type, @Param("owner") User user, Pageable pageable);

    @Override
    @RestResource(path = "findByFilterAndNotOwner", rel = "findByFilterAndNotOwner")
    @Query("select t from FstepFile t where not t.owner=:owner and t.filename like %:filter% and t.type=:type")
    Page<FstepFile> findByFilterAndNotOwner(@Param("filter") String filter, @Param("type") FstepFile.Type type, @Param("owner") User user, Pageable pageable);
}
