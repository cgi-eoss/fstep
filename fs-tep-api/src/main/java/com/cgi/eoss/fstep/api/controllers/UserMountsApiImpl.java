package com.cgi.eoss.fstep.api.controllers;

import com.cgi.eoss.fstep.model.QUser;
import com.cgi.eoss.fstep.model.QUserMount;
import com.cgi.eoss.fstep.model.UserMount;
import com.cgi.eoss.fstep.persistence.dao.UserMountDao;
import com.cgi.eoss.fstep.security.FstepSecurityService;
import com.querydsl.core.types.dsl.NumberPath;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Getter
@Component
public class UserMountsApiImpl extends BaseRepositoryApiImpl<UserMount>{

    private final FstepSecurityService securityService;
    private final UserMountDao dao;

    @Autowired
    public UserMountsApiImpl(FstepSecurityService securityService, UserMountDao dao) {
        this.securityService = securityService;
        this.dao = dao;
    }
    

    @Override
    NumberPath<Long> getIdPath() {
        return QUserMount.userMount.id;
    }

    @Override
    QUser getOwnerPath() {
        return QUserMount.userMount.owner;
    }

    @Override
    Class<UserMount> getEntityClass() {
        return UserMount.class;
    }
/*
    @Override
    public Page<UserMount> findByType(String type, Pageable pageable) {
        return getFilteredResults(QUserPreference.userPreference.type.eq(type), pageable);
    }
*/

}
