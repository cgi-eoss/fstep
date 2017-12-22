package com.cgi.eoss.fstep.api.controllers;

import com.cgi.eoss.fstep.catalogue.CatalogueService;
import com.cgi.eoss.fstep.model.Collection;
import com.cgi.eoss.fstep.model.QCollection;
import com.cgi.eoss.fstep.model.QUser;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.persistence.dao.CollectionDao;
import com.cgi.eoss.fstep.security.FstepSecurityService;
import com.google.common.base.Strings;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.NumberPath;
import java.io.IOException;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Getter
@Component
public class CollectionsApiImpl extends BaseRepositoryApiImpl<Collection> implements CollectionsApiCustom {

    private final FstepSecurityService securityService;
    private final CatalogueService catalogueService;
    private final CollectionDao dao;
    
    @Override
    NumberPath<Long> getIdPath() {
        return QCollection.collection.id;
    }

    @Override
    QUser getOwnerPath() {
        return QCollection.collection.owner;
    }

    @Override
    Class<Collection> getEntityClass() {
        return Collection.class;
    }

    @Override
    public <S extends Collection> S save(S collection) {
        try {
            if (collection.getOwner() == null) {
                getSecurityService().updateOwnerWithCurrentUser(collection);
            }
            collection.setIdentifier("fstep" + UUID.randomUUID().toString().replaceAll("-", ""));
            //TODO can be extended to ref data collections
            if (catalogueService.createOutputCollection(collection)) {
                return collection;
            }
            else {
                throw new RuntimeException("Failed to create requested collection ");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public void delete(Collection collection) {
        //TODO can be extended to ref data collections
        try {
            catalogueService.deleteOutputCollection(collection);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Page<Collection> findByFilterOnly(String filter, Pageable pageable) {
        return getFilteredResults(getFilterPredicate(filter), pageable);
    }

    @Override
    public Page<Collection> findByFilterAndOwner(String filter, User user, Pageable pageable) {
        return getFilteredResults(getOwnerPath().eq(user).and(getFilterPredicate(filter)), pageable);
    }

    @Override
    public Page<Collection> findByFilterAndNotOwner(String filter, User user, Pageable pageable) {
        return getFilteredResults(getOwnerPath().ne(user).and(getFilterPredicate(filter)), pageable);
    }

    private Predicate getFilterPredicate(String filter) {
        BooleanBuilder builder = new BooleanBuilder();

        if (!Strings.isNullOrEmpty(filter)) {
            builder.and(QCollection.collection.name.containsIgnoreCase(filter));
        }
        return builder.getValue();
    }
}
