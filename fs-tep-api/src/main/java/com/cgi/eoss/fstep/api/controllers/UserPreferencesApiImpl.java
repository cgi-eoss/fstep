package com.cgi.eoss.fstep.api.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import com.cgi.eoss.fstep.model.QUser;
import com.cgi.eoss.fstep.model.QUserPreference;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.model.UserPreference;
import com.cgi.eoss.fstep.persistence.dao.UserPreferenceDao;
import com.cgi.eoss.fstep.security.FstepSecurityService;
import com.google.common.base.Strings;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.NumberPath;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Getter
@Component
public class UserPreferencesApiImpl extends BaseRepositoryApiImpl<UserPreference>
        implements UserPreferencesApiCustom {

    // TODO move this to configuration
    private static final int MAX_PREFERENCES_PER_USER = 200;
    private final FstepSecurityService securityService;
    private final UserPreferenceDao dao;


    @Override
    public <S extends UserPreference> S save(S entity) {
        if (entity.getOwner() == null) {
            getSecurityService().updateOwnerWithCurrentUser(entity);
        }
        // Allow max items per user
        int currentPreferenceCount = getDao().findByOwner(entity.getOwner()).size();
        if (currentPreferenceCount >= MAX_PREFERENCES_PER_USER) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "User preference limit of " + MAX_PREFERENCES_PER_USER + " reached");
        }
        return getDao().save(entity);
    }


    @Override
    NumberPath<Long> getIdPath() {
        return QUserPreference.userPreference.id;
    }

    @Override
    QUser getOwnerPath() {
        return QUserPreference.userPreference.owner;
    }

    @Override
    Class<UserPreference> getEntityClass() {
        return UserPreference.class;
    }

    @Override
    public Page<UserPreference> findByType(String type, Pageable pageable) {
        return getFilteredResults(QUserPreference.userPreference.type.eq(type), pageable);
    }

    @Override
    public Page<UserPreference> findByName(String name, Pageable pageable) {
        return getFilteredResults(QUserPreference.userPreference.name.eq(name), pageable);
    }


    @Override
    public Page<UserPreference> search(User user, String name, String type, Pageable pageable) {
        return getFilteredResults(getPredicate(user, name, type), pageable);
    }
    
    private Predicate getPredicate(User user, String name, String type) {
        BooleanBuilder builder = new BooleanBuilder();
        if (user != null) {
            builder.and(getOwnerPath().eq(user));
        }
        if (!Strings.isNullOrEmpty(name)) {
            builder.and(QUserPreference.userPreference.name.eq(name));
        }
        
        if (!Strings.isNullOrEmpty(type)) {
            builder.and(QUserPreference.userPreference.type.eq(type));
        }

        return builder.getValue();
    }

}
