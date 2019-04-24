package com.cgi.eoss.fstep.subscriptions;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cgi.eoss.fstep.costing.CostingService;
import com.cgi.eoss.fstep.model.CostQuotation;
import com.cgi.eoss.fstep.model.CostQuotation.Recurrence;
import com.cgi.eoss.fstep.model.Quota;
import com.cgi.eoss.fstep.model.Subscription;
import com.cgi.eoss.fstep.model.Subscription.Status;
import com.cgi.eoss.fstep.persistence.service.QuotaDataService;
import com.cgi.eoss.fstep.persistence.service.SubscriptionDataService;
import com.cgi.eoss.fstep.quotas.UsageService;
import com.cgi.eoss.fstep.scheduledjobs.service.ScheduledJobService;
import com.cgi.eoss.fstep.scheduledjobs.service.ScheduledJobUtils;
import com.google.common.collect.ImmutableSet;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class SubscriptionServiceImpl implements SubscriptionService {

	private final CostingService costingService;
	private final SubscriptionDataService subscriptionDataService;
	private final ScheduledJobService scheduledJobService;
	private final QuotaDataService quotaDataService;
	private final UsageService usageService;

	@Autowired
	public SubscriptionServiceImpl(CostingService costingService, SubscriptionDataService subscriptionDataService,
			ScheduledJobService scheduledJobService, QuotaDataService quotaDataService, UsageService usageService) {
		this.costingService = costingService;
		this.subscriptionDataService = subscriptionDataService;
		this.scheduledJobService = scheduledJobService;
		this.quotaDataService = quotaDataService;
		this.usageService = usageService;
	}

	@Override
	public Subscription createSubscription(Subscription subscription) {
		validateSubscription(subscription);
		OffsetDateTime subscriptionCreationTime = OffsetDateTime.now();
		subscription.setCreated(subscriptionCreationTime);
		subscription.setCurrentStart(subscriptionCreationTime);
		subscription.setStatus(Status.ACTIVE);
		Quota quota = SubscriptionUtils.createSubscriptionQuota(subscription);
		quotaDataService.save(quota);
		subscription.setQuota(quota);
		subscription.setCurrentEnd(SubscriptionUtils.getNextPeriodEnd(subscription));
		subscription = subscriptionDataService.save(subscription);
		paySubscription(subscription);
		Set<String> cronExpressions = createCronExpression(subscription);
		if (cronExpressions != null) {
			Map<String, Object> jobContext = new HashMap<>();
			jobContext.put("subscriptionId", subscription.getId());
			scheduledJobService.scheduleCronsJob(SubscriptionChargeJob.class, getScheduledJobId(subscription),
					getScheduledJobGroup(subscription), jobContext, cronExpressions, getSchedulingStart(subscription),
					true);
		}
		return subscription;
	}

	private void paySubscription(Subscription subscription) {
		costingService.chargeForSubscription(subscription.getOwner().getWallet(), subscription);
	}

	private String getScheduledJobId(Subscription subscription) {
		return "subscription-" + subscription.getId();
	}

	private String getScheduledJobGroup(Subscription subscription) {
		return "subscriptions";
	}

	private void validateSubscription(Subscription subscription) {
		// There should be only one subscription per usage type per owner
		Subscription existingSubscription = subscriptionDataService
				.findByOwnerAndSubscriptionPlanUsageTypeAndStatusIsNot(subscription.getOwner(),
						subscription.getSubscriptionPlan().getUsageType(), Status.TERMINATED);
		if (existingSubscription != null) {
			throw new SubscriptionException("A subscription already exists for this usage type");
		}
		int requestedQuantity = subscription.getQuantity();
		if (requestedQuantity < subscription.getSubscriptionPlan().getMinQuantity()) {
			throw new SubscriptionException("Subscription quantity is less than the minimum allowed for this plan");
		}

		if (requestedQuantity > subscription.getSubscriptionPlan().getMaxQuantity()) {
			throw new SubscriptionException("Subscription quantity is more than the maximum allowed for this plan");
		}

		int cost = costingService.getSubscriptionCost(subscription).getCost();
		if (cost > subscription.getOwner().getWallet().getBalance()) {
			throw new SubscriptionException("Subscription cost (" + cost + " coins) exceeds current wallet balance");
		}
	}

	private Set<String> createCronExpression(Subscription subscription) {
		Recurrence recurrence = subscription.getSubscriptionPlan().getCostQuotation().getRecurrence();
		switch (recurrence) {
		case ONE_OFF:
			return null;
		case HOURLY:
			return ImmutableSet
					.of(ScheduledJobUtils.getCronExpressionEveryHourFromStart(subscription.getCurrentStart()));
		case MONTHLY:
			return ScheduledJobUtils.getCronExpressionsEveryMonthFromStart(subscription.getCurrentStart());
		default:
			throw new SubscriptionException("Unsupported subscription recurrence: " + recurrence.toString());
		}
	}

	private Date getSchedulingStart(Subscription subscription) {
		Recurrence recurrence = subscription.getSubscriptionPlan().getCostQuotation().getRecurrence();
		switch (recurrence) {
		case ONE_OFF:
			return null;
		case HOURLY:
			// Start the scheduling after half hour - enough to skip the first hour
			return new Date(subscription.getCreated().plusMinutes(30).toInstant().toEpochMilli());
		case MONTHLY:
			// Start the scheduling after one week - enough to skip the first month
			return new Date(subscription.getCreated().plusDays(7).toInstant().toEpochMilli());
		default:
			throw new SubscriptionException("Unsupported subscription recurrence: " + recurrence.toString());
		}
	}

	@Override
	public void deactivateSubscription(Subscription subscription) {
		if (!subscription.getStatus().equals(Status.ACTIVE)) {
			throw new SubscriptionException("Subscription is not active");
		}
		subscription.setRenew(false);
		subscriptionDataService.save(subscription);
	}

	@Override
	public void terminateSubscription(Subscription subscription) {
		if (!subscription.getStatus().equals(Status.ACTIVE)) {
			throw new SubscriptionException("Subscription is not active");
		}
		Quota quota = subscription.getQuota();
		subscription.setQuota(null);
		subscription.setEnded(subscription.getCurrentEnd());
		subscription.setStatus(Status.TERMINATED);
		subscriptionDataService.save(subscription);
		if (quota != null) {
			quotaDataService.delete(quota);
		}
		scheduledJobService.unscheduleJob(getScheduledJobId(subscription), getScheduledJobGroup(subscription));
		scheduledJobService.deleteJob(getScheduledJobId(subscription), getScheduledJobGroup(subscription));
	}

	@Override
	public Subscription changeSubscription(Subscription oldSubscription, Subscription newSubscription) {
		if (!oldSubscription.getStatus().equals(Status.ACTIVE)) {
			throw new SubscriptionException("Only active subscriptions can be upgraded");
		}
		if (!oldSubscription.getSubscriptionPlan().getUsageType()
				.equals(newSubscription.getSubscriptionPlan().getUsageType())) {
			throw new SubscriptionException("Incompatible subscription plans");
		}
		if (oldSubscription.getSubscriptionPlan().getCostQuotation().getRecurrence() != newSubscription
				.getSubscriptionPlan().getCostQuotation().getRecurrence()) {
			throw new SubscriptionException("Upgrade to subscription with different recurrence is not supported");
		}
		newSubscription.setCurrentStart(oldSubscription.getCurrentStart());
		newSubscription.setCurrentEnd(oldSubscription.getCurrentEnd());
		newSubscription.setCreated(oldSubscription.getCreated());

		// Check if this is an upgrade, a downgrade or a same cost subscription
		int oldCost = getCost(oldSubscription);
		int newCost = getCost(newSubscription);
		int deltaCost = newCost - oldCost;
		if (deltaCost > 0) {
			return handleSubscriptionUpgrade(oldSubscription, newSubscription);
		} else if (deltaCost == 0) {
			return handleSameCostSubscription(oldSubscription, newSubscription);
		} else {
			return handleSubscriptionDowngrade(oldSubscription, newSubscription);
		}

	}

	private Subscription handleSameCostSubscription(Subscription oldSubscription, Subscription newSubscription) {
		// Check new quota is not less than current usage
		Quota newQuota = SubscriptionUtils.createSubscriptionQuota(newSubscription);
		Optional<Long> usage = usageService.getUsage(oldSubscription.getOwner(), newQuota.getUsageType());
		if (usage.isPresent() && usage.get() > newQuota.getValue()) {
			throw new SubscriptionException("Subscription quota (" + newQuota + ") is smaller than the current usage");
		}
		quotaDataService.save(newQuota);
		newSubscription.setQuota(newQuota);
		newSubscription.setStatus(Status.ACTIVE);
		newSubscription.setRenew(true);
		newSubscription.setDowngradePlan(null);
		newSubscription.setDowngradeQuantity(null);
		return subscriptionDataService.save(newSubscription);
	}

	private Subscription handleSubscriptionUpgrade(Subscription oldSubscription, Subscription newSubscription) {
		int deltaCost = getDeltaCost(oldSubscription, newSubscription);
		if (deltaCost > oldSubscription.getOwner().getWallet().getBalance()) {
			throw new SubscriptionException(
					"Subscription cost (" + deltaCost + " coins) exceeds current wallet balance");
		}

		// Check new quota is not less than current usage
		Quota newQuota = SubscriptionUtils.createSubscriptionQuota(newSubscription);
		Optional<Long> usage = usageService.getUsage(oldSubscription.getOwner(), newQuota.getUsageType());
		if (usage.isPresent() && usage.get() > newQuota.getValue()) {
			throw new SubscriptionException("Subscription quota (" + newQuota + ") is smaller than the current usage");
		}
		quotaDataService.save(newQuota);
		newSubscription.setQuota(newQuota);
		newSubscription.setStatus(Status.ACTIVE);
		newSubscription.setRenew(true);
		newSubscription.setDowngradePlan(null);
		newSubscription.setDowngradeQuantity(null);
		costingService.transactForSubscription(oldSubscription.getOwner().getWallet(), oldSubscription, deltaCost * -1);
		return subscriptionDataService.save(newSubscription);
	}

	private Subscription handleSubscriptionDowngrade(Subscription oldSubscription, Subscription newSubscription) {
		// Check new quota will not be less than current usage
		Quota newQuota = SubscriptionUtils.createSubscriptionQuota(newSubscription);
		Optional<Long> usage = usageService.getUsage(oldSubscription.getOwner(), newQuota.getUsageType());
		if (usage.isPresent() && usage.get() > newQuota.getValue()) {
			throw new SubscriptionException("Subscription quota (" + newQuota + ") is smaller than the current usage");
		}
		oldSubscription.setRenew(true);
		oldSubscription.setDowngradePlan(newSubscription.getSubscriptionPlan());
		oldSubscription.setDowngradeQuantity(newSubscription.getQuantity());
		return subscriptionDataService.save(oldSubscription);
	}

	private int getDeltaCost(Subscription oldSubscription, Subscription newSubscription) {
		CostQuotation costQuotation = oldSubscription.getSubscriptionPlan().getCostQuotation();
		switch (costQuotation.getRecurrence()) {
		case MONTHLY: {
			return getUnusedMonthlyCost(newSubscription) - getUnusedMonthlyCost(oldSubscription);
		}
		case HOURLY: {
			return getCost(newSubscription) - getCost (oldSubscription);
		}
		default:
			return 0;
		}
	}

	private int getUnusedMonthlyCost(Subscription subscription) {
		CostQuotation costQuotation = subscription.getSubscriptionPlan().getCostQuotation();
		OffsetDateTime now = OffsetDateTime.now();
		double daysUnused = Duration.between(now, subscription.getCurrentEnd()).toDays();
		double daysTotal = Duration.between(subscription.getCurrentStart(), subscription.getCurrentEnd()).toDays();
		double cost = subscription.getQuantity() * costQuotation.getCost() * daysUnused / daysTotal;
		return (int) Math.round(Math.ceil(cost));
	}
	
	private int getCost(Subscription subscription) {
		return subscription.getQuantity() * subscription.getSubscriptionPlan().getCostQuotation().getCost();
	}
	
	@Override
	public void blockSubscription(Subscription subscription) {
		subscription.setStatus(Status.BLOCKED);
		Quota quota = subscription.getQuota();
		subscription.setQuota(null);
		subscriptionDataService.save(subscription);
		if (quota != null) {
			quotaDataService.delete(quota);
		}
	}

	@Override
	public void unblockSubscription(Subscription subscription) {
		if (!subscription.getStatus().equals(Status.BLOCKED)) {
			throw new SubscriptionException("Subscription is not blocked");
		}
		while (subscription.getCurrentEnd().isBefore(OffsetDateTime.now())) {
			int cost = costingService.getSubscriptionCost(subscription).getCost();
			if (cost > subscription.getOwner().getWallet().getBalance()) {
				throw new SubscriptionException(
						"Subscription cost (" + cost + " coins) exceeds current wallet balance");
			}
			paySubscription(subscription);
			subscription.setCurrentStart(subscription.getCurrentEnd());
			subscription.setCurrentEnd(SubscriptionUtils.getNextPeriodEnd(subscription));
			subscription = subscriptionDataService.save(subscription);
		}
		subscription.setStatus(Status.ACTIVE);
		Quota quota = SubscriptionUtils.createSubscriptionQuota(subscription);
		quotaDataService.save(quota);
		subscription.setQuota(quota);
		subscriptionDataService.save(subscription);
	}

	@Override
	public Subscription downgradeSubscription(Subscription subscription) {
		// Check new quota will not be less than current usage
		subscription.setSubscriptionPlan(subscription.getDowngradePlan());
		subscription.setQuantity(subscription.getDowngradeQuantity());
		Quota newQuota = SubscriptionUtils.createSubscriptionQuota(subscription);
		Optional<Long> usage = usageService.getUsage(subscription.getOwner(), newQuota.getUsageType());
		if (usage.isPresent() && usage.get() > newQuota.getValue()) {
			throw new SubscriptionException("Subscription quota (" + newQuota + ") is smaller than the current usage");
		}
		subscription.setDowngradePlan(null);
		subscription.setDowngradeQuantity(null);
		quotaDataService.save(newQuota);
		subscription.setQuota(newQuota);
		return subscriptionDataService.save(subscription);
	}

}
