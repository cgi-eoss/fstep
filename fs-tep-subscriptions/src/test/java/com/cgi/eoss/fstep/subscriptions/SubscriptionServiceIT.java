package com.cgi.eoss.fstep.subscriptions;

import java.time.OffsetDateTime;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.cgi.eoss.fstep.model.BillingScheme;
import com.cgi.eoss.fstep.model.CostQuotation;
import com.cgi.eoss.fstep.model.CostQuotation.Recurrence;
import com.cgi.eoss.fstep.model.Subscription;
import com.cgi.eoss.fstep.model.SubscriptionPlan;
import com.cgi.eoss.fstep.model.UsageType;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.model.Wallet;
import com.cgi.eoss.fstep.persistence.service.SubscriptionPlanDataService;
import com.cgi.eoss.fstep.persistence.service.UserDataService;
import com.google.common.collect.ImmutableSet;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {SubscriptionsConfig.class})
@TestPropertySource("classpath:test-subscriptions.properties")
@Transactional
public class SubscriptionServiceIT {
    
	
    @Autowired
    private SubscriptionPlanDataService subscriptionPlanDataService;
    
    @Autowired
    private UserDataService userService;
    
    @Autowired
    private SubscriptionService subscriptionService;
    
    @Test
    public void testCreate() {
    	User owner = new User("owner-uid");
    	userService.save(ImmutableSet.of(owner));
        SubscriptionPlan plan = new SubscriptionPlan("Storage Basic", UsageType.FILES_STORAGE_MB, 1000, 1, 99, BillingScheme.UNIT, new CostQuotation(5, Recurrence.MONTHLY));
        subscriptionPlanDataService.save(ImmutableSet.of(plan));

        Subscription subscription = new Subscription(owner, plan, 20, OffsetDateTime.now());
        subscriptionService.createSubscription(subscription);
        
    }
    
    @Test
    public void testChange() {
    	User owner = new User("owner-uid");
    	Wallet wallet = new Wallet(owner);
        wallet.setBalance(1000);
        owner.setWallet(wallet);
        userService.save(ImmutableSet.of(owner));
        SubscriptionPlan plan = new SubscriptionPlan("Storage Basic", UsageType.FILES_STORAGE_MB, 1000, 1, 99, BillingScheme.UNIT, new CostQuotation(5, Recurrence.MONTHLY));
        subscriptionPlanDataService.save(ImmutableSet.of(plan));

        Subscription subscription = new Subscription(owner, plan, 20, OffsetDateTime.now());
        subscriptionService.createSubscription(subscription);
        Subscription newSubscription = new Subscription(owner, plan, 30, OffsetDateTime.now());
        subscriptionService.changeSubscription(subscription, newSubscription);
    }
    
   

}