package com.cgi.eoss.fstep.api.controllers;

import java.time.LocalDateTime;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.cgi.eoss.fstep.model.Job;
import com.cgi.eoss.fstep.model.Job.Status;
import com.cgi.eoss.fstep.model.QJob;
import com.cgi.eoss.fstep.model.QUser;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.persistence.dao.JobDao;
import com.cgi.eoss.fstep.security.FstepSecurityService;
import com.google.common.base.Strings;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberPath;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Getter
@Component
public class JobsApiImpl extends BaseRepositoryApiImpl<Job> implements JobsApiCustom {

    private final FstepSecurityService securityService;
    private final JobDao dao;

    @Override
    NumberPath<Long> getIdPath() {
        return QJob.job.id;
    }

    @Override
    QUser getOwnerPath() {
        return QJob.job.owner;
    }

    @Override
    Class<Job> getEntityClass() {
        return Job.class;
    }

    BooleanExpression isNotSubjob() {
        return QJob.job.parentJob.isNull();
    }
    
    BooleanExpression isChildOf(Long parentId) {
        return QJob.job.parentJob.id.eq(parentId);
    }
    
    @Override
    public Page<Job> findByFilterOnly(String filter, Collection<Status> statuses, Pageable pageable) {
        return getFilteredResults(getFilterPredicate(filter, statuses), pageable);
    }

    @Override
    public Page<Job> findByFilterAndOwner(String filter, Collection<Status> statuses, User user, Pageable pageable) {
        return getFilteredResults(getOwnerPath().eq(user).and(getFilterPredicate(filter, statuses)), pageable);
    }

    @Override
    public Page<Job> findByFilterAndNotOwner(String filter, Collection<Status> statuses, User user, Pageable pageable) {
        return getFilteredResults(getOwnerPath().ne(user).and(getFilterPredicate(filter, statuses)), pageable);
    }
    
    @Override
    public Page<Job> findByFilterAndIsNotSubjob(String filter, Collection<Status> statuses, Pageable pageable){
        return getFilteredResults(isNotSubjob().and(getFilterPredicate(filter, statuses)), pageable);
    }
    
    @Override
    public Page<Job> findByFilterAndIsNotSubjobAndOwner(String filter, Collection<Status> statuses, User user, Pageable pageable){
        return getFilteredResults(isNotSubjob().and(getOwnerPath().eq(user)).and(getFilterPredicate(filter, statuses)), pageable);
    }

    @Override
    public Page<Job> findByFilterAndIsNotSubjobAndNotOwner(String filter, Collection<Status> statuses, User user, Pageable pageable){
        return getFilteredResults(isNotSubjob().and(getOwnerPath().ne(user)).and(getFilterPredicate(filter, statuses)), pageable);
    }

    @Override
    public Page<Job> findByFilterAndParent(String filter, Collection<Status> statuses, Long parentId, Pageable pageable){
        return getFilteredResults(getFilterPredicate(filter, statuses, parentId), pageable);
    }

    @Override
    public Page<Job> findByFilterAndParentAndOwner(String filter, Collection<Status> statuses, Long parentId, User user, Pageable pageable){
        return getFilteredResults((getOwnerPath().eq(user)).and(getFilterPredicate(filter, statuses, parentId)), pageable);
    }

    @Override
    public Page<Job> findByFilterAndParentAndNotOwner(String filter, Collection<Status> statuses, Long parentId, User user, Pageable pageable){
        return getFilteredResults((getOwnerPath().ne(user)).and(getFilterPredicate(filter, statuses, parentId)), pageable);
    }

    private Predicate getFilterPredicate(String filter, Collection<Status> statuses) {
        BooleanBuilder builder = new BooleanBuilder(Expressions.asBoolean(true).isTrue());

        if (!Strings.isNullOrEmpty(filter)) {
            builder.and(QJob.job.id.stringValue().contains(filter)
                    .or(QJob.job.config.label.containsIgnoreCase(filter))
                    .or(QJob.job.config.service.name.containsIgnoreCase(filter)));
        }

        if (statuses != null && !statuses.isEmpty()) {
            builder.and(QJob.job.status.in(statuses));
        }

        return builder.getValue();
    }
    
    private Predicate getFilterPredicate(String filter, Collection<Status> statuses, Long parentId) {
        BooleanBuilder builder = new BooleanBuilder(Expressions.asBoolean(true).isTrue());

        if (!Strings.isNullOrEmpty(filter)) {
            builder.and(QJob.job.id.stringValue().contains(filter)
                    .or(QJob.job.config.label.containsIgnoreCase(filter))
                    .or(QJob.job.config.service.name.containsIgnoreCase(filter)));
        }

        if (statuses != null && !statuses.isEmpty()) {
            builder.and(QJob.job.status.in(statuses));
        }
        
        if (parentId != null){
            builder.and(isChildOf(parentId));
        }

        return builder.getValue();
    }

	@Override
	public Page<Job> parametricFind(String filter, Collection<Status> statuses, Long parentId, User owner,
			User notOwner, String inputIdentifier, LocalDateTime startDateTime, LocalDateTime endDateTime,
			Pageable pageable){
		Predicate p = buildPredicate(filter, statuses, parentId, owner, notOwner, inputIdentifier, startDateTime, endDateTime);
		return getFilteredResults(p, pageable);
	}

	private Predicate buildPredicate(String filter, Collection<Status> statuses, Long parentId,
			User owner, User notOwner, String inputIdentifier, LocalDateTime startDateTime,
			LocalDateTime endDateTime) {
		BooleanBuilder builder = new BooleanBuilder(Expressions.asBoolean(true).isTrue());
		if (!Strings.isNullOrEmpty(filter)) {
			builder.and(QJob.job.id.stringValue().toLowerCase().contains(filter.toLowerCase()));
		}
		if (statuses != null && !statuses.isEmpty()) {
			builder.and(QJob.job.status.in(statuses));
		}
		if (parentId != null) {
			builder.and(isChildOf(parentId));
		}
		else {
			builder.and(QJob.job.parentJob.isNull());
		}
		if (owner != null) {
			builder.and(getOwnerPath().eq(owner));
		}
		if (notOwner !=null) {
			builder.and(getOwnerPath().ne(notOwner));
		}
		if (startDateTime != null && endDateTime != null) {
			builder.and(QJob.job.startTime.before(endDateTime)).and(QJob.job.endTime.after(startDateTime));
		}
		else if (startDateTime != null) {
			builder.and(QJob.job. startTime.before(startDateTime)).and(QJob.job.endTime.after(startDateTime));
		}
		else if (endDateTime != null){
			builder.and(QJob.job.startTime.before(endDateTime)).and(QJob.job.endTime.after(endDateTime));
		}
		if (!Strings.isNullOrEmpty(inputIdentifier)) {
			builder.and(Expressions.booleanTemplate("{0} like concat('%', {1}, '%')", QJob.job.config.inputs, inputIdentifier));
		}
		return builder.getValue();
	}

}
