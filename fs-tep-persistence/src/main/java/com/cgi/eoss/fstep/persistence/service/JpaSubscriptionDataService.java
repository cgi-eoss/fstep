package com.cgi.eoss.fstep.persistence.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cgi.eoss.fstep.model.Subscription;
import com.cgi.eoss.fstep.model.Subscription.Status;
import com.cgi.eoss.fstep.model.UsageType;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.persistence.dao.FstepEntityDao;
import com.cgi.eoss.fstep.persistence.dao.SubscriptionDao;
import com.querydsl.core.types.Predicate;
import static com.cgi.eoss.fstep.model.QSubscription.subscription;

@Service
@Transactional(readOnly = true)
public class JpaSubscriptionDataService extends AbstractJpaDataService<Subscription> implements SubscriptionDataService{

	
	private final SubscriptionDao subscriptionDao;

    @Autowired
    public JpaSubscriptionDataService(SubscriptionDao subscriptionDao) {
        this.subscriptionDao = subscriptionDao;
    }
	
	@Override
	FstepEntityDao<Subscription> getDao() {
		return subscriptionDao;
	}

	@Override
	Predicate getUniquePredicate(Subscription entity) {
		return subscription.owner.eq(entity.getOwner()).and(subscription.subscriptionPlan.eq(entity.getSubscriptionPlan()).and(subscription.created.eq(entity.getCreated())));
	}

	@Override
	public Subscription findByOwnerAndSubscriptionPlanUsageTypeAndStatusIsNot(User owner, UsageType usageType, Status status) {
		return subscriptionDao.findByOwnerAndSubscriptionPlanUsageTypeAndStatusIsNot(owner, usageType, status);
	}

  
}
