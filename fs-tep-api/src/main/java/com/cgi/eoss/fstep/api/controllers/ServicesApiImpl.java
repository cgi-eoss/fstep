package com.cgi.eoss.fstep.api.controllers;

import com.cgi.eoss.fstep.security.FstepSecurityService;
import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.QFstepService;
import com.cgi.eoss.fstep.model.QUser;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.persistence.dao.FstepServiceDao;
import com.google.common.base.Strings;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.NumberPath;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Getter
@Component
public class ServicesApiImpl extends BaseRepositoryApiImpl<FstepService> implements ServicesApiCustom {

    private final FstepSecurityService securityService;
    private final FstepServiceDao dao;

    @Override
    NumberPath<Long> getIdPath() {
        return QFstepService.fstepService.id;
    }

    @Override
    QUser getOwnerPath() {
        return QFstepService.fstepService.owner;
    }

    @Override
    Class<FstepService> getEntityClass() {
        return FstepService.class;
    }

    @Override
    public Page<FstepService> findByFilterOnly(String filter, FstepService.Type serviceType, Pageable pageable) {
        return getFilteredResults(getFilterPredicate(filter, serviceType), pageable);
    }

    @Override
    public Page<FstepService> findByFilterAndOwner(String filter, User user, FstepService.Type serviceType, Pageable pageable) {
        return getFilteredResults(getOwnerPath().eq(user).and(getFilterPredicate(filter, serviceType)), pageable);
    }

    @Override
    public Page<FstepService> findByFilterAndNotOwner(String filter, User user, FstepService.Type serviceType, Pageable pageable) {
        return getFilteredResults(getOwnerPath().ne(user).and(getFilterPredicate(filter, serviceType)), pageable);
    }

    private Predicate getFilterPredicate(String filter, FstepService.Type serviceType) {
        BooleanBuilder builder = new BooleanBuilder();

        if (!Strings.isNullOrEmpty(filter)) {
            builder.and(QFstepService.fstepService.name.containsIgnoreCase(filter).or(QFstepService.fstepService.description.containsIgnoreCase(filter)));
        }

        if (serviceType != null) {
            builder.and(QFstepService.fstepService.type.eq(serviceType));
        }

        return builder.getValue();
    }

}
