package com.cgi.eoss.fstep.api.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cgi.eoss.fstep.model.JobProcessing;
import com.cgi.eoss.fstep.model.QJobProcessing;
import com.cgi.eoss.fstep.model.QUser;
import com.cgi.eoss.fstep.persistence.dao.JobProcessingDao;
import com.cgi.eoss.fstep.security.FstepSecurityService;
import com.querydsl.core.types.dsl.NumberPath;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Getter
@Component
public class JobProcessingsApiImpl extends BaseRepositoryApiImpl<JobProcessing> {

    private final FstepSecurityService securityService;
    private final JobProcessingDao dao;

    @Override
    NumberPath<Long> getIdPath() {
        return QJobProcessing.jobProcessing.id;
    }

    @Override
    QUser getOwnerPath() {
        return QJobProcessing.jobProcessing.job.owner;
    }

    @Override
    Class<JobProcessing> getEntityClass() {
        return JobProcessing.class;
    }
}
