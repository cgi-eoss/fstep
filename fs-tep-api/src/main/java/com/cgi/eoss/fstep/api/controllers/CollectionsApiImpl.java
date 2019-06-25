package com.cgi.eoss.fstep.api.controllers;

import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.cgi.eoss.fstep.catalogue.CatalogueService;
import com.cgi.eoss.fstep.model.Collection;
import com.cgi.eoss.fstep.model.FstepFile.Type;
import com.cgi.eoss.fstep.model.QCollection;
import com.cgi.eoss.fstep.model.QUser;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.persistence.dao.CollectionDao;
import com.cgi.eoss.fstep.security.FstepSecurityService;
import com.google.common.base.Strings;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberPath;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

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
        if (collection.getOwner() == null) {
            getSecurityService().updateOwnerWithCurrentUser(collection);
        }
        if (collection.getFileType() == null) {
        	throw new RuntimeException("Collection file type is mandatory");
        }
        if (collection.getIdentifier() == null) {
            collection.setIdentifier("fstep" + UUID.randomUUID().toString().replaceAll("-", ""));
            try {
	            catalogueService.createCollection(collection);
	        }
            catch (IOException e) {
            	throw new RuntimeException("Failed to create underlying collection ");
            }
        }
        return dao.save(collection);
    }
    
    @Override
    public void delete(Collection collection) {
        try {
			catalogueService.deleteCollection(collection);
		    dao.delete(collection);
		} catch (IOException e) {
			throw new RuntimeException("Failed to delete underlying collection");
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
    
    @Override
	public Page<Collection> parametricFind(String filter, Type fileType, User owner, User notOwner,
			Pageable pageable) {
		Predicate p = buildPredicate(filter, fileType, owner, notOwner);
		return getFilteredResults(p, pageable);
	}
	
	private Predicate buildPredicate(String filter, Type fileType, User owner, User notOwner) {
		BooleanBuilder builder = new BooleanBuilder(Expressions.asBoolean(true).isTrue());
		if (!Strings.isNullOrEmpty(filter)) {
			builder.and(QCollection.collection.id.stringValue().toLowerCase().contains(filter.toLowerCase()).
					or(QCollection.collection.name.stringValue().toLowerCase().contains(filter.toLowerCase())));
		}
		if (fileType != null) {
			builder.and(QCollection.collection.fileType.eq(fileType));
		}
		if (owner != null) {
			builder.and(getOwnerPath().eq(owner));
		}
		if (notOwner !=null) {
			builder.and(getOwnerPath().ne(notOwner));
		}
		return builder.getValue();
	}
}
