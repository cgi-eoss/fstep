package com.cgi.eoss.fstep.api.controllers;

import com.cgi.eoss.fstep.security.FstepSecurityService;
import com.cgi.eoss.fstep.model.FstepServiceContextFile;
import com.cgi.eoss.fstep.model.QFstepServiceContextFile;
import com.cgi.eoss.fstep.model.QUser;
import com.cgi.eoss.fstep.persistence.dao.FstepServiceContextFileDao;
import com.google.common.io.BaseEncoding;
import com.querydsl.core.types.dsl.NumberPath;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Getter
@Component
public class ServiceFilesApiImpl extends BaseRepositoryApiImpl<FstepServiceContextFile> implements ServiceFilesApiCustom {

    private final FstepSecurityService securityService;
    private final FstepServiceContextFileDao dao;

    @Override
    NumberPath<Long> getIdPath() {
        return QFstepServiceContextFile.fstepServiceContextFile.id;
    }

    @Override
    QUser getOwnerPath() {
        return QFstepServiceContextFile.fstepServiceContextFile.service.owner;
    }

    @Override
    Class<FstepServiceContextFile> getEntityClass() {
        return FstepServiceContextFile.class;
    }

    @Override
    public <S extends FstepServiceContextFile> S save(S serviceFile) {
        // Transform base64 content into real content
        serviceFile.setContent(new String(BaseEncoding.base64().decode(serviceFile.getContent())));
        return getDao().save(serviceFile);
    }

}
