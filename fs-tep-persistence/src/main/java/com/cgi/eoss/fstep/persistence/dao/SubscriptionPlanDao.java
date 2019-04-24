package com.cgi.eoss.fstep.persistence.dao;

import java.util.List;

import com.cgi.eoss.fstep.model.SubscriptionPlan;

public interface SubscriptionPlanDao extends FstepEntityDao<SubscriptionPlan> {

	List<SubscriptionPlan> findByNameContainingIgnoreCase(String term);
}
