package com.cgi.eoss.fstep.api.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.cgi.eoss.fstep.api.ApiConfig;
import com.cgi.eoss.fstep.api.ApiTestConfig;
import com.cgi.eoss.fstep.model.CostingExpression;
import com.cgi.eoss.fstep.model.CostingExpression.Type;
import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.Role;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.persistence.service.CostingExpressionDataService;
import com.cgi.eoss.fstep.persistence.service.ServiceDataService;
import com.cgi.eoss.fstep.persistence.service.UserDataService;
import com.google.common.collect.ImmutableSet;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {ApiConfig.class, ApiTestConfig.class})
@AutoConfigureMockMvc
@TestPropertySource("classpath:test-api.properties")
@Transactional
public class CostApiIT {

    @Autowired
    private CostingExpressionDataService dataService;

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private ServiceDataService serviceDataService;

    @Autowired
    private CostingExpressionDataService costingExpressionDataService;

    @Autowired
    private MockMvc mockMvc;
    
    
    private User fstepUser;
    private User fstepAdmin;

	private FstepService svc;

	private FstepService svc2;
    
    @Before
    public void setUp() {
        fstepUser = new User("fstep-user");
        fstepUser.setRole(Role.USER);
        fstepAdmin = new User("fstep-admin");
        fstepAdmin.setRole(Role.ADMIN);

        userDataService.save(ImmutableSet.of(fstepUser, fstepAdmin));
        svc = new FstepService("fstepService", fstepUser, "dockerTag");
        svc2 = new FstepService("fstepService2", fstepUser, "dockerTag");
        serviceDataService.save(ImmutableSet.of(svc, svc2));
        CostingExpression costingExpression = new CostingExpression(Type.SERVICE, svc2.getId(), "5", "5");
        costingExpressionDataService.save(costingExpression);
    }

    @After
    public void tearDown() {
        dataService.deleteAll();
    }

    @Test
    public void testGetDefaultCostingExpression() throws Exception {
        mockMvc.perform(get("/api/cost/service/" + svc.getId())
                .header("REMOTE_USER", fstepUser.getName())).andExpect(status().isOk())
                .andExpect(jsonPath("$.costExpression").value("1"));
    }
    
    @Test
    public void testGetCustomCostingExpression() throws Exception {
	    mockMvc.perform(get("/api/cost/service/" + svc2.getId())
	            .header("REMOTE_USER", fstepUser.getName())).andExpect(status().isOk())
	            .andExpect(jsonPath("$.costExpression").value("5"));
    }
}
