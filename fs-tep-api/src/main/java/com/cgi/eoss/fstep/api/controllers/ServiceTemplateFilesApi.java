package com.cgi.eoss.fstep.api.controllers;

import java.util.List;

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

import com.cgi.eoss.fstep.model.FstepServiceTemplate;
import com.cgi.eoss.fstep.model.FstepServiceTemplateFile;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.model.projections.ShortFstepServiceTemplateFile;

@RepositoryRestResource(
        path = "serviceTemplateFiles",
        itemResourceRel = "serviceTemplateFile",
        collectionResourceRel = "serviceTemplateFiles",
        excerptProjection = ShortFstepServiceTemplateFile.class
)
public interface ServiceTemplateFilesApi extends BaseRepositoryApi<FstepServiceTemplateFile>, ServiceFilesApiCustom, PagingAndSortingRepository<FstepServiceTemplateFile, Long> {

    @Override
    @RestResource(exported = false)
    List<FstepServiceTemplateFile> findAll();

    @Override
    @PostAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(returnObject.serviceTemplate, 'read')")
    FstepServiceTemplateFile findOne(Long id);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#serviceTemplateFile.serviceTemplate, 'write')")
    <S extends FstepServiceTemplateFile> S save(@P("serviceTemplateFile") S serviceTemplateFile);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    <S extends FstepServiceTemplateFile> Iterable<S> save(@P("serviceTemplateFiles") Iterable<S> serviceTemplateFiles);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#serviceFile.serviceTemplate, 'administration')")
    void delete(@P("serviceFile") FstepServiceTemplateFile serviceFile);

    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#serviceTemplate, 'read')")
    Page<FstepServiceTemplateFile> findByServiceTemplate(@Param("serviceTemplate") FstepServiceTemplate serviceTemplate, Pageable pageable);

    @Override
    @Query("select t from FstepServiceTemplateFile t where t.serviceTemplate.owner=:owner")
    Page<FstepServiceTemplateFile> findByOwner(@Param("owner") User user, Pageable pageable);

    @Override
    @Query("select t from FstepServiceTemplateFile t where not t.serviceTemplate.owner=:owner")
    Page<FstepServiceTemplateFile> findByNotOwner(@Param("owner") User user, Pageable pageable);

}
