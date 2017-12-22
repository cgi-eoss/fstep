package com.cgi.eoss.fstep.api.controllers;

import com.cgi.eoss.fstep.model.QUser;
import com.cgi.eoss.fstep.model.QUserEndpoint;
import com.cgi.eoss.fstep.model.UserEndpoint;
import com.cgi.eoss.fstep.persistence.dao.UserEndpointDao;
import com.cgi.eoss.fstep.security.FstepSecurityService;
import com.querydsl.core.types.dsl.NumberPath;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Getter
@Component
public class UserEndpointsApiImpl extends BaseRepositoryApiImpl<UserEndpoint>{

    private final FstepSecurityService securityService;
    private final UserEndpointDao dao;

    @Autowired
    public UserEndpointsApiImpl(FstepSecurityService securityService, UserEndpointDao dao) {
        this.securityService = securityService;
        this.dao = dao;
    }
    

    @Override
    NumberPath<Long> getIdPath() {
        return QUserEndpoint.userEndpoint.id;
    }

    @Override
    QUser getOwnerPath() {
        return QUserEndpoint.userEndpoint.owner;
    }

    @Override
    Class<UserEndpoint> getEntityClass() {
        return UserEndpoint.class;
    }
/*
    @Override
    public Page<UserMount> findByType(String type, Pageable pageable) {
        return getFilteredResults(QUserPreference.userPreference.type.eq(type), pageable);
    }
*/

}
