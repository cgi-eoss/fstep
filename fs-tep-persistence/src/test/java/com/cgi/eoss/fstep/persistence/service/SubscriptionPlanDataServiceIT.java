package com.cgi.eoss.fstep.persistence.service;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

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
import com.cgi.eoss.fstep.model.SubscriptionPlan;
import com.cgi.eoss.fstep.model.UsageType;
import com.cgi.eoss.fstep.persistence.PersistenceConfig;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {PersistenceConfig.class})
@TestPropertySource("classpath:test-persistence.properties")
@Transactional
public class SubscriptionPlanDataServiceIT {
    @Autowired
    private SubscriptionPlanDataService dataService;
    
    @Test
    public void test() throws Exception {
        SubscriptionPlan plan = new SubscriptionPlan("Storage Basic", UsageType.FILES_STORAGE_MB, 1000, 1, 99, BillingScheme.UNIT, new CostQuotation(5, Recurrence.MONTHLY));
        SubscriptionPlan plan2 = new SubscriptionPlan("Storage Medium", UsageType.FILES_STORAGE_MB, 1000, 100, 1000, BillingScheme.UNIT, new CostQuotation(4, Recurrence.MONTHLY));
    	dataService.save(ImmutableSet.of(plan, plan2));

        assertThat(dataService.getAll(), is(ImmutableList.of(plan, plan2)));
        assertThat(dataService.getById(plan.getId()), is(plan));
        assertThat(dataService.getByIds(ImmutableSet.of(plan.getId())), is(ImmutableList.of(plan)));
        assertThat(dataService.isUniqueAndValid(new SubscriptionPlan("Storage Basic", UsageType.FILES_STORAGE_MB, 1000, 1, 99, BillingScheme.UNIT, new CostQuotation(5, Recurrence.MONTHLY))), is(false));
        assertThat(dataService.isUniqueAndValid(new SubscriptionPlan("Storage Advanced", UsageType.FILES_STORAGE_MB, 1000, 100, 1000, BillingScheme.UNIT, new CostQuotation(4, Recurrence.MONTHLY))), is(true));

        assertThat(dataService.search("Storage"), is(ImmutableList.of(plan, plan2)));
    }

}