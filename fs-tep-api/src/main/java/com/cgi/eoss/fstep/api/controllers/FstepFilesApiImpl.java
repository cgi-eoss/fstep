package com.cgi.eoss.fstep.api.controllers;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.cgi.eoss.fstep.catalogue.CatalogueService;
import com.cgi.eoss.fstep.model.Collection;
import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.Job;
import com.cgi.eoss.fstep.model.QFstepFile;
import com.cgi.eoss.fstep.model.QJob;
import com.cgi.eoss.fstep.model.QUser;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.persistence.dao.FstepFileDao;
import com.cgi.eoss.fstep.security.FstepSecurityService;
import com.google.common.base.Strings;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.SubQueryExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.JPAExpressions;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Getter
@Component
public class FstepFilesApiImpl extends BaseRepositoryApiImpl<FstepFile> implements FstepFilesApiCustom {

    private final FstepSecurityService securityService;
    private final FstepFileDao dao;
    private final CatalogueService catalogueService;

    @Override
    NumberPath<Long> getIdPath() {
        return QFstepFile.fstepFile.id;
    }

    @Override
    QUser getOwnerPath() {
        return QFstepFile.fstepFile.owner;
    }

    @Override
    Class<FstepFile> getEntityClass() {
        return FstepFile.class;
    }

    @Override
    public void delete(FstepFile fstepFile) {
        try {
            catalogueService.delete(fstepFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Page<FstepFile> findByType(FstepFile.Type type, Pageable pageable) {
        return getFilteredResults(getFilterPredicate(null, type), pageable);
    }

    @Override
    public Page<FstepFile> findByFilterOnly(String filter, FstepFile.Type type, Pageable pageable) {
        return getFilteredResults(getFilterPredicate(filter, type), pageable);
    }

    @Override
    public Page<FstepFile> findByFilterAndOwner(String filter, FstepFile.Type type, User user, Pageable pageable) {
        return getFilteredResults(getOwnerPath().eq(user).and(getFilterPredicate(filter, type)).and(QFstepFile.fstepFile.type.eq(type)),
                pageable);
    }

    @Override
    public Page<FstepFile> findByFilterAndNotOwner(String filter, FstepFile.Type type, User user, Pageable pageable) {
        return getFilteredResults(getOwnerPath().ne(user)
                        .and(getFilterPredicate(filter, type)).and(QFstepFile.fstepFile.type.eq(type)),
                pageable);
    }

    private Predicate getFilterPredicate(String filter, FstepFile.Type type) {
        BooleanBuilder builder = new BooleanBuilder();

        if (!Strings.isNullOrEmpty(filter)) {
            builder.and(QFstepFile.fstepFile.filename.containsIgnoreCase(filter));
        }

        if (type != null) {
            builder.and(QFstepFile.fstepFile.type.eq(type));
        }

        return builder.getValue();
    }
    
    @Override
	public Page<FstepFile> parametricFind(String filter, Collection collection, FstepFile.Type type, User owner,
			User notOwner, Job job, Pageable pageable){
		Predicate p = buildPredicate(filter, collection, type, owner, notOwner, job);
		return getFilteredResults(p, pageable);
	}

	private Predicate buildPredicate(String filter, Collection collection, FstepFile.Type type, User owner,
			User notOwner, Job job) {
		BooleanBuilder builder = new BooleanBuilder();
		if (!Strings.isNullOrEmpty(filter)) {
			builder.and(QFstepFile.fstepFile.filename.stringValue().toLowerCase().contains(filter.toLowerCase()));
		}
		if (collection != null) {
			builder.and(QFstepFile.fstepFile.collection.eq(collection));
		}
		if (type != null) {
			builder.and(QFstepFile.fstepFile.type.eq(type));
		}
		if (owner != null) {
			builder.and(getOwnerPath().eq(owner));
		}
		if (notOwner !=null) {
			builder.and(getOwnerPath().ne(notOwner));
		}
		if (job != null) {
			SubQueryExpression<Long> jobOutputFilesExpression = JPAExpressions.select(QFstepFile.fstepFile.id)
					.from(QJob.job).innerJoin(QJob.job.outputFiles, QFstepFile.fstepFile)
					.where(QJob.job.id.eq((job.getId())));
			builder.and(QFstepFile.fstepFile.id.in(jobOutputFilesExpression));
		}
		return builder.getValue();
	}


}
