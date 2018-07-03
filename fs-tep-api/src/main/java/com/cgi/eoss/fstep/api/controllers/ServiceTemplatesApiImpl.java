package com.cgi.eoss.fstep.api.controllers;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.cgi.eoss.fstep.model.DefaultServiceTemplate;
import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.FstepService.Type;
import com.cgi.eoss.fstep.model.FstepServiceTemplate;
import com.cgi.eoss.fstep.model.QFstepServiceTemplate;
import com.cgi.eoss.fstep.model.QUser;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.persistence.dao.FstepServiceTemplateDao;
import com.cgi.eoss.fstep.persistence.service.DefaultServiceTemplateDataService;
import com.cgi.eoss.fstep.security.FstepSecurityService;
import com.google.common.base.Strings;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Getter
@Component
public class ServiceTemplatesApiImpl extends BaseRepositoryApiImpl<FstepServiceTemplate> implements ServiceTemplatesApiCustom {

    private final FstepSecurityService securityService;
    private final FstepServiceTemplateDao dao;
    private final DefaultServiceTemplateDataService defaultTemplateDataService;
    

    @Override
    NumberPath<Long> getIdPath() {
        return QFstepServiceTemplate.fstepServiceTemplate.id;
    }
    
    @Override
    QUser getOwnerPath() {
        return QFstepServiceTemplate.fstepServiceTemplate.owner;
    }

    @Override
    Class<FstepServiceTemplate> getEntityClass() {
        return FstepServiceTemplate.class;
    }

    @Override
    public Page<FstepServiceTemplate> findByFilterOnly(String filter, FstepService.Type serviceType, Pageable pageable) {
        return getFilteredResults(getFilterPredicate(filter, serviceType), pageable);
    }

    @Override
    public Page<FstepServiceTemplate> findByFilterAndOwner(String filter, User user, FstepService.Type serviceType, Pageable pageable) {
        return getFilteredResults(getOwnerPath().eq(user).and(getFilterPredicate(filter, serviceType)), pageable);
    }

    @Override
    public Page<FstepServiceTemplate> findByFilterAndNotOwner(String filter, User user, FstepService.Type serviceType, Pageable pageable) {
        return getFilteredResults(getOwnerPath().ne(user).and(getFilterPredicate(filter, serviceType)), pageable);
    }

    private Predicate getFilterPredicate(String filter, FstepService.Type serviceType) {
        BooleanBuilder builder = new BooleanBuilder();

        if (!Strings.isNullOrEmpty(filter)) {
            builder.and(QFstepServiceTemplate.fstepServiceTemplate.name.containsIgnoreCase(filter).or(QFstepServiceTemplate.fstepServiceTemplate.description.containsIgnoreCase(filter)));
        }

        if (serviceType != null) {
            builder.and(QFstepServiceTemplate.fstepServiceTemplate.type.eq(serviceType));
        }

        return builder.getValue();
    }

	@Override
	public FstepServiceTemplate getDefaultByType(Type serviceType) {
		DefaultServiceTemplate defaultFstepServiceTemplate = defaultTemplateDataService.getByServiceType(serviceType);
		if (defaultFstepServiceTemplate != null) {
			return defaultFstepServiceTemplate.getServiceTemplate();
		}   
		
		Set<Long> visibleIds = getSecurityService().getVisibleObjectIds(getEntityClass(), getDao().findAllIds());
        BooleanExpression isVisibleAndOfType = getIdPath().in(visibleIds).and(QFstepServiceTemplate.fstepServiceTemplate.type.eq(serviceType));
        return getDao().findAll(isVisibleAndOfType).stream().findFirst().orElse(null);
	}
}
