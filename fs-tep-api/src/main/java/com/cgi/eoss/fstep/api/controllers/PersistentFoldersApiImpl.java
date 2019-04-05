package com.cgi.eoss.fstep.api.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cgi.eoss.fstep.model.PersistentFolder;
import com.cgi.eoss.fstep.model.QPersistentFolder;
import com.cgi.eoss.fstep.model.QUser;
import com.cgi.eoss.fstep.persistence.dao.PersistentFolderDao;
import com.cgi.eoss.fstep.security.FstepSecurityService;
import com.querydsl.core.types.dsl.NumberPath;

import lombok.Getter;

@Getter
@Component
public class PersistentFoldersApiImpl extends BaseRepositoryApiImpl<PersistentFolder>{

    private final FstepSecurityService securityService;
    private final PersistentFolderDao dao;

    @Autowired
    public PersistentFoldersApiImpl(FstepSecurityService securityService, PersistentFolderDao dao) {
        this.securityService = securityService;
        this.dao = dao;
    }
    

    @Override
    NumberPath<Long> getIdPath() {
        return QPersistentFolder.persistentFolder.id;
    }

    @Override
    QUser getOwnerPath() {
        return QPersistentFolder.persistentFolder.owner;
    }

    @Override
    Class<PersistentFolder> getEntityClass() {
        return PersistentFolder.class;
    }


}
