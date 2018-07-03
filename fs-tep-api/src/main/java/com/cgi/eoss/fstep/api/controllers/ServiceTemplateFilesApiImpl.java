package com.cgi.eoss.fstep.api.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cgi.eoss.fstep.model.FstepServiceTemplateFile;
import com.cgi.eoss.fstep.model.QFstepServiceTemplateFile;
import com.cgi.eoss.fstep.model.QUser;
import com.cgi.eoss.fstep.persistence.dao.FstepServiceTemplateFileDao;
import com.cgi.eoss.fstep.security.FstepSecurityService;
import com.google.common.io.BaseEncoding;
import com.querydsl.core.types.dsl.NumberPath;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Getter
@Component
public class ServiceTemplateFilesApiImpl extends BaseRepositoryApiImpl<FstepServiceTemplateFile> implements ServiceTemplateFilesApiCustom {

    private final FstepSecurityService securityService;
    private final FstepServiceTemplateFileDao dao;

    @Override
    NumberPath<Long> getIdPath() {
        return QFstepServiceTemplateFile.fstepServiceTemplateFile.id;
    }

    @Override
    QUser getOwnerPath() {
        return QFstepServiceTemplateFile.fstepServiceTemplateFile.serviceTemplate.owner;
    }

    @Override
    Class<FstepServiceTemplateFile> getEntityClass() {
        return FstepServiceTemplateFile.class;
    }

    @Override
    public <S extends FstepServiceTemplateFile> S save(S serviceTemplateFile) {
        // Transform base64 content into real content
    	serviceTemplateFile.setContent(new String(BaseEncoding.base64().decode(serviceTemplateFile.getContent())));
        return getDao().save(serviceTemplateFile);
    }

}
