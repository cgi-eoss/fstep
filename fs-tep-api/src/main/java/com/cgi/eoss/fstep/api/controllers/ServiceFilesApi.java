package com.cgi.eoss.fstep.api.controllers;

import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.FstepServiceContextFile;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.model.projections.ShortFstepServiceContextFile;
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

import java.util.List;

@RepositoryRestResource(
        path = "serviceFiles",
        itemResourceRel = "serviceFile",
        collectionResourceRel = "serviceFiles",
        excerptProjection = ShortFstepServiceContextFile.class
)
public interface ServiceFilesApi extends BaseRepositoryApi<FstepServiceContextFile>, ServiceFilesApiCustom, PagingAndSortingRepository<FstepServiceContextFile, Long> {

    @Override
    @RestResource(exported = false)
    List<FstepServiceContextFile> findAll();

    @Override
    @PostAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(returnObject.service, 'write')")
    FstepServiceContextFile findOne(Long id);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#serviceFile.service, 'write')")
    <S extends FstepServiceContextFile> S save(@P("serviceFile") S serviceFile);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    <S extends FstepServiceContextFile> Iterable<S> save(@P("serviceFiles") Iterable<S> serviceFiles);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#serviceFile.service, 'administration')")
    void delete(@P("serviceFile") FstepServiceContextFile serviceFile);

    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#service, 'write')")
    Page<FstepServiceContextFile> findByService(@Param("service") FstepService service, Pageable pageable);

    @Override
    @Query("select t from FstepServiceContextFile t where t.service.owner=:owner")
    Page<FstepServiceContextFile> findByOwner(@Param("owner") User user, Pageable pageable);

    @Override
    @Query("select t from FstepServiceContextFile t where not t.service.owner=:owner")
    Page<FstepServiceContextFile> findByNotOwner(@Param("owner") User user, Pageable pageable);

}
