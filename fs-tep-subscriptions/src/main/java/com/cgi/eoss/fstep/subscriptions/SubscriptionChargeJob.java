package com.cgi.eoss.fstep.subscriptions;

import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cgi.eoss.fstep.costing.CostingService;
import com.cgi.eoss.fstep.model.Quota;
import com.cgi.eoss.fstep.model.Subscription;
import com.cgi.eoss.fstep.model.Subscription.Status;
import com.cgi.eoss.fstep.persistence.service.SubscriptionDataService;
import com.cgi.eoss.fstep.scheduledjobs.service.ScheduledJob;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class SubscriptionChargeJob extends ScheduledJob{

	@Autowired
	private SubscriptionDataService subscriptionDataService;
	
	@Autowired
	private CostingService costingService;
	
	@Autowired
	private SubscriptionService subscriptionService;
	
	
	@Override
	public void executeJob(Map<String, Object> jobContext) {
		Long subscriptionId = (Long) jobContext.get("subscriptionId");
		Subscription subscription = subscriptionDataService.getById(subscriptionId);
		processSubscription(subscription);
	}

	private void processSubscription(Subscription subscription) {
		if (subscription.getStatus().equals(Status.BLOCKED)){
			return;
		}
		
		if (subscription.getDowngradePlan() != null) {
			try {
				subscription = subscriptionService.downgradeSubscription(subscription);
			}
			catch (Exception e) {
				LOG.error("Failed Subscription downgrade ignored", e);
			}
		}
		
		if (!subscription.isRenew()) {
			subscriptionService.terminateSubscription(subscription);
			return;
		}
		
		int cost = costingService.getSubscriptionCost(subscription).getCost();
		if (cost > subscription.getOwner().getWallet().getBalance()) {
			subscriptionService.deactivateSubscription(subscription);
			subscriptionService.terminateSubscription(subscription);
			return;
        }
		costingService.chargeForSubscription(subscription.getOwner().getWallet(), subscription);
		subscription.setCurrentStart(subscription.getCurrentEnd());
		subscription.setCurrentEnd(SubscriptionUtils.getNextPeriodEnd(subscription));
		subscriptionDataService.save(subscription);
		LOG.info("Charged subscription {}" + subscription.getId());
	}
	
}
