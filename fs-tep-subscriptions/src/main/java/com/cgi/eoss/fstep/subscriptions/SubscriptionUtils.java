package com.cgi.eoss.fstep.subscriptions;

import java.time.OffsetDateTime;

import com.cgi.eoss.fstep.model.CostQuotation.Recurrence;
import com.cgi.eoss.fstep.model.Quota;
import com.cgi.eoss.fstep.model.Subscription;

public class SubscriptionUtils {
	
	private SubscriptionUtils() {
		
	}
	
	public static Quota createSubscriptionQuota(Subscription subscription) {
		Quota quota = new Quota();
		quota.setOwner(subscription.getOwner());
		quota.setValue(subscription.getQuantity() * subscription.getSubscriptionPlan().getUnit());
		quota.setUsageType(subscription.getSubscriptionPlan().getUsageType());
		return quota;
	}
	
	public static OffsetDateTime getNextPeriodEnd(Subscription subscription) {
		Recurrence recurrence = subscription.getSubscriptionPlan().getCostQuotation().getRecurrence();
		switch (recurrence) {
		case ONE_OFF:
			return null;
		case HOURLY:
			return subscription.getCurrentStart().plusHours(1);
		case MONTHLY: 
			return getNextMonthDay(subscription);
		default:
			throw new SubscriptionException("Unsupported subscription recurrence: " + recurrence.toString());
		}
	}

	private static OffsetDateTime getNextMonthDay(Subscription subscription) {
		int creationDayOfMonth = subscription.getCreated().getDayOfMonth();
		if (creationDayOfMonth == 29 || creationDayOfMonth == 30) {
			OffsetDateTime nextEnd = subscription.getCurrentStart().plusMonths(1);
			int monthLength = nextEnd.getMonth().length(nextEnd.toLocalDate().isLeapYear());
			return nextEnd.withDayOfMonth(creationDayOfMonth <= monthLength ? creationDayOfMonth : nextEnd.getDayOfMonth()); 
		}
		return subscription.getCurrentStart().plusMonths(1);
	}
}
