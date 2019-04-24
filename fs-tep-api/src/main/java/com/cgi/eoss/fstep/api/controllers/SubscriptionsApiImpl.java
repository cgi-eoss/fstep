package com.cgi.eoss.fstep.api.controllers;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cgi.eoss.fstep.model.QSubscription;
import com.cgi.eoss.fstep.model.QUser;
import com.cgi.eoss.fstep.model.Subscription;
import com.cgi.eoss.fstep.persistence.dao.SubscriptionDao;
import com.cgi.eoss.fstep.security.FstepSecurityService;
import com.cgi.eoss.fstep.subscriptions.SubscriptionService;
import com.querydsl.core.types.dsl.NumberPath;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Getter
@Component
public class SubscriptionsApiImpl extends BaseRepositoryApiImpl<Subscription> implements SubscriptionsApiCustom {

    private final FstepSecurityService securityService;
    private final SubscriptionService subscriptionService;
    private final SubscriptionDao dao;
    @PersistenceContext
    private EntityManager entityManager;
    
    @Override
    NumberPath<Long> getIdPath() {
        return QSubscription.subscription.id;
    }

    @Override
    QUser getOwnerPath() {
        return QSubscription.subscription.owner;
    }

    @Override
    Class<Subscription> getEntityClass() {
        return Subscription.class;
    }

    @SuppressWarnings("unchecked")
	@Override
    public <S extends Subscription> S save(S subscription) {
        if (subscription.getOwner() == null) {
            getSecurityService().updateOwnerWithCurrentUser(subscription);
        }
        if (subscription.getId() != null) {
        	entityManager.detach(subscription);
        	Subscription oldSubscription = dao.findOne(subscription.getId());
       		return (S) subscriptionService.changeSubscription(oldSubscription, subscription);
        }
    	return (S) subscriptionService.createSubscription(subscription);
    }
    
    @Override
    public void delete(Subscription subscription) {
    	subscriptionService.terminateSubscription(subscription);
    }
}
