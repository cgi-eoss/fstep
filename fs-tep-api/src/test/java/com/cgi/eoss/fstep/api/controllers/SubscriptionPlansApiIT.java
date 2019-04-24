package com.cgi.eoss.fstep.api.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.cgi.eoss.fstep.api.ApiConfig;
import com.cgi.eoss.fstep.api.ApiTestConfig;
import com.cgi.eoss.fstep.model.BillingScheme;
import com.cgi.eoss.fstep.model.CostQuotation;
import com.cgi.eoss.fstep.model.CostQuotation.Recurrence;
import com.cgi.eoss.fstep.model.Role;
import com.cgi.eoss.fstep.model.SubscriptionPlan;
import com.cgi.eoss.fstep.model.UsageType;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.persistence.service.SubscriptionPlanDataService;
import com.cgi.eoss.fstep.persistence.service.UserDataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {ApiConfig.class, ApiTestConfig.class})
@AutoConfigureMockMvc
@TestPropertySource("classpath:test-api.properties")
@Transactional
public class SubscriptionPlansApiIT {

    @Autowired
    private SubscriptionPlanDataService dataService;

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private User fstepUser;
    private User fstepAdmin;

    private SubscriptionPlan plan;
    
    @Before
    public void setUp() {
        fstepUser = new User("fstep-user");
        fstepUser.setRole(Role.USER);
        fstepAdmin = new User("fstep-admin");
        fstepAdmin.setRole(Role.ADMIN);

        userDataService.save(ImmutableSet.of(fstepUser, fstepAdmin));
        
        plan = new SubscriptionPlan("Storage Basic", UsageType.FILES_STORAGE_MB, 1000, 1, 99, BillingScheme.UNIT, new CostQuotation(5, Recurrence.MONTHLY));

        SubscriptionPlan plan2 = new SubscriptionPlan("Storage Medium", UsageType.FILES_STORAGE_MB, 1000, 100, 1000, BillingScheme.UNIT, new CostQuotation(4, Recurrence.MONTHLY));
    	
        dataService.save(ImmutableSet.of(plan, plan2));
    }

    @After
    public void tearDown() {
        dataService.deleteAll();
    }

    @Test
    public void testFindAll() throws Exception {
        mockMvc.perform(get("/api/subscriptionPlans/")
                .header("REMOTE_USER", fstepUser.getName())).andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.subscriptionPlans").isArray())
                .andExpect(jsonPath("$._embedded.subscriptionPlans.length()").value(2))
                .andExpect(jsonPath("$._embedded.subscriptionPlans[0].name").value("Storage Basic"));
    }

    @Test
    public void testCreateRequiresAdmin() throws Exception {
    	SubscriptionPlan plan = new SubscriptionPlan("Storage Advanced", UsageType.FILES_STORAGE_MB, 1000, 100, 1000, BillingScheme.UNIT, new CostQuotation(4, Recurrence.MONTHLY));
    	
        mockMvc.perform(post("/api/subscriptionPlans/")
                .header("REMOTE_USER", fstepUser.getName())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(plan))        		)
        		.andExpect(status().isForbidden());
    }
    
    @Test
    public void testCreate() throws Exception {
    	SubscriptionPlan plan = new SubscriptionPlan("Storage Advanced", UsageType.FILES_STORAGE_MB, 1000, 100, 1000, BillingScheme.UNIT, new CostQuotation(4, Recurrence.MONTHLY));
    	mockMvc.perform(post("/api/subscriptionPlans/")
                .header("REMOTE_USER", fstepAdmin.getName())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(plan))        		)
        		.andExpect(status().isCreated());
    }
    
    @Test
    public void testUpdate() throws Exception {
    	SubscriptionPlan savedPlan = dataService.findOneByExample(plan);
    	savedPlan.setDescription("New description");
    	mockMvc.perform(patch("/api/subscriptionPlans/" + savedPlan.getId())
    	        .header("REMOTE_USER", fstepAdmin.getName())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(savedPlan))        		)
        		.andExpect(status().isNoContent());
    }

}
