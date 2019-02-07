package com.cgi.eoss.fstep.persistence.service;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.cgi.eoss.fstep.model.Quota;
import com.cgi.eoss.fstep.model.UsageType;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.persistence.PersistenceConfig;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {PersistenceConfig.class})
@TestPropertySource("classpath:test-persistence.properties")
@Transactional
public class QuotaDataServiceIT {
    
	@Autowired
    private UserDataService userService;

    @Autowired
    private QuotaDataService dataService;
    
    @Test
    public void test() throws Exception {
        User owner = new User("owner-uid");
        User owner2 = new User("owner-uid2");
        
        userService.save(ImmutableSet.of(owner, owner2));

        Quota quota = new Quota();
        quota.setUsageType(UsageType.MAX_RUNNABLE_JOBS);
        quota.setOwner(owner);
        quota.setValue(10L);
        
        dataService.save(ImmutableList.of(quota));
        assertThat(dataService.getByOwnerAndUsageType(owner, UsageType.MAX_RUNNABLE_JOBS).getValue(), is(10L));
        assertNull(dataService.getByOwnerAndUsageType(owner2, UsageType.MAX_RUNNABLE_JOBS));
    }

}