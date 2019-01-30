package com.cgi.eoss.fstep.api.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.cgi.eoss.fstep.model.FstepEntityWithOwner;
import com.cgi.eoss.fstep.model.QUser;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.persistence.dao.FstepEntityDao;
import com.cgi.eoss.fstep.security.FstepSecurityService;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.SubQueryExpression;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;

public abstract class BaseRepositoryApiImpl<T extends FstepEntityWithOwner<T>> implements BaseRepositoryApi<T> {

    @Override
    public <S extends T> S save(S entity) {
        if (entity.getOwner() == null) {
            getSecurityService().updateOwnerWithCurrentUser(entity);
        }
        return getDao().save(entity);
    }

    @Override
    public Page<T> findAll(Pageable pageable) {
        if (getSecurityService().isSuperUser()) {
            return getDao().findAll(pageable);
        } else {
            SubQueryExpression<Long> visibleQuery = getSecurityService().getVisibleQuery(getEntityClass());
            BooleanExpression isVisible = getIdPath().in(visibleQuery);
            return getDao().findAll(isVisible, pageable);
        }
    }

    @Override
    public Page<T> findByOwner(User user, Pageable pageable) {
        return getFilteredResults(getOwnerPath().eq(user), pageable);
    }

    @Override
    public Page<T> findByNotOwner(User user, Pageable pageable) {
        return getFilteredResults(getOwnerPath().ne(user), pageable);
    }

    Page<T> getFilteredResults(Predicate predicate, Pageable pageable) {
        if (getSecurityService().isSuperUser()) {
            return getDao().findAll(predicate, pageable);
        } else {
        	SubQueryExpression<Long> visibleQuery = getSecurityService().getVisibleQuery(getEntityClass());
            BooleanExpression isVisible = getIdPath().in(visibleQuery);
            return getDao().findAll(isVisible.and(predicate), pageable);
        }
    }

    abstract NumberPath<Long> getIdPath();

    abstract QUser getOwnerPath();

    abstract Class<T> getEntityClass();

    abstract FstepSecurityService getSecurityService();

    abstract FstepEntityDao<T> getDao();

}
