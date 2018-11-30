package com.cgi.eoss.fstep.api.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cgi.eoss.fstep.model.QSystematicProcessing;
import com.cgi.eoss.fstep.model.QUser;
import com.cgi.eoss.fstep.model.SystematicProcessing;
import com.cgi.eoss.fstep.persistence.dao.SystematicProcessingDao;
import com.cgi.eoss.fstep.security.FstepSecurityService;
import com.querydsl.core.types.dsl.NumberPath;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Getter
@Component
public class SystematicProcessingsApiImpl extends BaseRepositoryApiImpl<SystematicProcessing>  {

    private final FstepSecurityService securityService;
    private final SystematicProcessingDao dao;

    @Override
    NumberPath<Long> getIdPath() {
        return QSystematicProcessing.systematicProcessing.id;
    }

    @Override
    QUser getOwnerPath() {
        return QSystematicProcessing.systematicProcessing.owner;
    }

    @Override
    Class<SystematicProcessing> getEntityClass() {
        return SystematicProcessing.class;
    }

}
