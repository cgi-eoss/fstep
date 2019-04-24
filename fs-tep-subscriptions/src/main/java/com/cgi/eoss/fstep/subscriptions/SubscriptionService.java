package com.cgi.eoss.fstep.subscriptions;

import com.cgi.eoss.fstep.model.Subscription;

public interface SubscriptionService {

	Subscription createSubscription(Subscription subscription);

	void deactivateSubscription(Subscription subscription);
	
	void terminateSubscription(Subscription subscription);
	
	Subscription changeSubscription(Subscription oldSubscription, Subscription newSubscription);
	
	void blockSubscription(Subscription subscription);
	
	void unblockSubscription(Subscription subscription);
	
	Subscription downgradeSubscription(Subscription subscription);

}