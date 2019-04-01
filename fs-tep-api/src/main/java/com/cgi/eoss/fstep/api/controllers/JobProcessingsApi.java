package com.cgi.eoss.fstep.api.controllers;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.security.access.prepost.PostAuthorize;

import com.cgi.eoss.fstep.model.JobProcessing;

@RepositoryRestResource(path = "jobprocessings", itemResourceRel = "jobprocessing", collectionResourceRel = "jobprocessings")
public interface JobProcessingsApi extends BaseRepositoryApi<JobProcessing>, PagingAndSortingRepository<JobProcessing, Long> {

    @Override
    @PostAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(returnObject, 'read')")
    JobProcessing findOne(Long id);

    // Users cannot create job processing instances via the API; they managed by the platform
    @Override
    @RestResource(exported = false)
    <S extends JobProcessing> Iterable<S> save(Iterable<S> jobs);

    // Users cannot create job instances via the API; they are managed by the platform
    @Override
    @RestResource(exported = false)
    <S extends JobProcessing> S save(S job);

}
