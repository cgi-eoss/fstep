package com.cgi.eoss.fstep.api.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.cgi.eoss.fstep.model.QQuota;
import com.cgi.eoss.fstep.model.QUser;
import com.cgi.eoss.fstep.model.Quota;
import com.cgi.eoss.fstep.model.UsageType;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.persistence.dao.QuotaDao;
import com.cgi.eoss.fstep.security.FstepSecurityService;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Getter
@Component
public class QuotasApiImpl extends BaseRepositoryApiImpl<Quota> implements QuotasApiCustom{

    private final FstepSecurityService securityService;
    private final QuotaDao dao;
    
    @Override
    NumberPath<Long> getIdPath() {
        return QQuota.quota.id;
    }

    @Override
    QUser getOwnerPath() {
        return QQuota.quota.owner;
    }

    @Override
    Class<Quota> getEntityClass() {
        return Quota.class;
    }

    @Override
    public Page<Quota> findAll(Pageable pageable) {
        if (getSecurityService().isAdmin()) {
            return getDao().findAll(pageable);
        } else {
            BooleanExpression isOwned = QQuota.quota.owner.eq(getSecurityService().getCurrentUser());
            return getDao().findAll(isOwned, pageable);
        }
    }

	@Override
	public Quota findByUsageTypeAndOwner(UsageType usageType, User user) {
		return dao.getByOwnerAndUsageType(user, usageType);
	}
}
