package com.cgi.eoss.fstep.persistence.service;

import static com.cgi.eoss.fstep.model.QSubscriptionPlan.subscriptionPlan;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cgi.eoss.fstep.model.SubscriptionPlan;
import com.cgi.eoss.fstep.persistence.dao.FstepEntityDao;
import com.cgi.eoss.fstep.persistence.dao.SubscriptionPlanDao;
import com.querydsl.core.types.Predicate;

@Service
@Transactional(readOnly = true)
public class JpaSubscriptionPlanDataService extends AbstractJpaDataService<SubscriptionPlan> implements SubscriptionPlanDataService{

	
	private final SubscriptionPlanDao subscriptionPlanDao;

    @Autowired
    public JpaSubscriptionPlanDataService(SubscriptionPlanDao subscriptionPlanDao) {
        this.subscriptionPlanDao = subscriptionPlanDao;
    }
	
	@Override
	FstepEntityDao<SubscriptionPlan> getDao() {
		return subscriptionPlanDao;
	}

	@Override
	Predicate getUniquePredicate(SubscriptionPlan entity) {
		return subscriptionPlan.name.eq(entity.getName());
		
	}

	@Override
	public List<SubscriptionPlan> search(String term) {
		return subscriptionPlanDao.findByNameContainingIgnoreCase(term);
	}

  
}
