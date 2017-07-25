package com.cgi.eoss.fstep.api.controllers;

import com.cgi.eoss.fstep.security.FstepSecurityService;
import com.cgi.eoss.fstep.model.Group;
import com.cgi.eoss.fstep.model.QGroup;
import com.cgi.eoss.fstep.model.QUser;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.persistence.dao.GroupDao;
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
public class GroupsApiImpl extends BaseRepositoryApiImpl<Group> implements GroupsApiCustom {

    private final FstepSecurityService securityService;
    private final GroupDao dao;

    @Override
    NumberPath<Long> getIdPath() {
        return QGroup.group.id;
    }

    @Override
    QUser getOwnerPath() {
        return QGroup.group.owner;
    }

    @Override
    Class<Group> getEntityClass() {
        return Group.class;
    }

    @Override
    public Page<Group> findByFilterOnly(String filter, Pageable pageable) {
        return getFilteredResults(
                getFilterPredicate(filter),
                pageable);
    }

    @Override
    public Page<Group> findByFilterAndOwner(String filter, User user, Pageable pageable) {
        return getFilteredResults(getOwnerPath().eq(user).and(getFilterPredicate(filter)), pageable);
    }

    @Override
    public Page<Group> findByFilterAndNotOwner(String filter, User user, Pageable pageable) {
        return getFilteredResults(getOwnerPath().ne(user).and(getFilterPredicate(filter)), pageable);
    }

    private Predicate getFilterPredicate(String filter) {
        BooleanBuilder builder = new BooleanBuilder();

        if (!Strings.isNullOrEmpty(filter)) {
            builder.and(QGroup.group.name.containsIgnoreCase(filter)
                    .or(QGroup.group.description.containsIgnoreCase(filter)));
        }

        return builder.getValue();
    }

}
