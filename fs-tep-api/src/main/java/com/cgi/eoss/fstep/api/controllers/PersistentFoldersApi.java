package com.cgi.eoss.fstep.api.controllers;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;

import com.cgi.eoss.fstep.model.PersistentFolder;

@RepositoryRestResource(path = "persistentfolders", itemResourceRel = "persistentfolder", collectionResourceRel = "persistentfolders")
public interface PersistentFoldersApi extends BaseRepositoryApi<PersistentFolder>, PagingAndSortingRepository<PersistentFolder, Long> {

    @Override
    @PostAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(returnObject, 'read')")
    PersistentFolder findOne(Long id);

    //Only admins can create/delete persistent folders
    @Override
    @PreAuthorize("hasAnyRole('ADMIN')")
    <S extends PersistentFolder> S save(@P("persistentFolder") S persistentFolder);
    

    @Override
    @PreAuthorize("hasAnyRole('ADMIN')")
    <S extends PersistentFolder> Iterable<S> save(Iterable<S> persistentFolders);

}
