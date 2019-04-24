package com.cgi.eoss.fstep.persistence.service;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

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
import com.cgi.eoss.fstep.model.Subscription.Status;
import com.cgi.eoss.fstep.model.SubscriptionPlan;
import com.cgi.eoss.fstep.model.UsageType;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.persistence.PersistenceConfig;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {PersistenceConfig.class})
@TestPropertySource("classpath:test-persistence.properties")
@Transactional
public class SubscriptionDataServiceIT {
    
	@Autowired
    private SubscriptionPlanDataService subscriptionPlanDataService;
    
    @Autowired
    private UserDataService userService;
    
    @Autowired
    private SubscriptionDataService dataService;
    
    @Test
    public void test() {
    	User owner = new User("owner-uid");
        User owner2 = new User("owner-uid2");
        userService.save(ImmutableSet.of(owner, owner2));

        SubscriptionPlan plan = new SubscriptionPlan("Storage Basic", UsageType.FILES_STORAGE_MB, 1000, 1, 99, BillingScheme.UNIT, new CostQuotation(5, Recurrence.MONTHLY));
        SubscriptionPlan plan2 = new SubscriptionPlan("Storage Medium", UsageType.FILES_STORAGE_MB, 1000, 100, 1000, BillingScheme.UNIT, new CostQuotation(4, Recurrence.MONTHLY));
    	subscriptionPlanDataService.save(ImmutableSet.of(plan, plan2));

        Subscription subscription = new Subscription(owner, plan, 20, OffsetDateTime.now());
        dataService.save(subscription);
        
        Subscription subscription2 = new Subscription(owner2, plan2, 50, OffsetDateTime.now());
        dataService.save(subscription2);
        
        
        assertThat(dataService.getAll(), is(ImmutableList.of(subscription, subscription2)));
        assertThat(dataService.getById(subscription.getId()), is(subscription));
        assertThat(dataService.getByIds(ImmutableSet.of(subscription.getId())), is(ImmutableList.of(subscription)));
       
       
    }
    
    @Test
    public void testFindByPlanUsageType() {
    	User owner = new User("owner-uid");
        userService.save(ImmutableSet.of(owner));

        SubscriptionPlan plan = new SubscriptionPlan("Storage Basic", UsageType.FILES_STORAGE_MB, 1000, 1, 99, BillingScheme.UNIT, new CostQuotation(5, Recurrence.MONTHLY));
        subscriptionPlanDataService.save(ImmutableSet.of(plan));

        Subscription subscription = new Subscription(owner, plan, 20, OffsetDateTime.now());
        dataService.save(subscription);
        
        assertThat(dataService.findByOwnerAndSubscriptionPlanUsageTypeAndStatusIsNot(subscription.getOwner(), UsageType.FILES_STORAGE_MB, Status.TERMINATED), is(subscription));
    }

}