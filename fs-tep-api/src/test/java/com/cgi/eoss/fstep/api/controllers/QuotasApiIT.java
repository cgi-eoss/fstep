package com.cgi.eoss.fstep.api.controllers;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.cgi.eoss.fstep.api.ApiConfig;
import com.cgi.eoss.fstep.api.ApiTestConfig;
import com.cgi.eoss.fstep.model.Quota;
import com.cgi.eoss.fstep.model.Role;
import com.cgi.eoss.fstep.model.UsageType;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.persistence.service.QuotaDataService;
import com.cgi.eoss.fstep.persistence.service.UserDataService;
import com.google.common.collect.ImmutableSet;
import com.jayway.jsonpath.JsonPath;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {ApiConfig.class, ApiTestConfig.class})
@AutoConfigureMockMvc
@TestPropertySource("classpath:test-api.properties")
@Transactional
public class QuotasApiIT {
	
	private static final JsonPath USER_HREF_JSONPATH = JsonPath.compile("$._links.self.href");
    
    @Autowired
    private QuotaDataService dataService;

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private MockMvc mockMvc;

    private User fstepUser1;
    private User fstepUser2;
    private User fstepUser3;
    private User fstepUser4;
    private User fstepAdmin;
    
    @Before
    public void setUp() {
        fstepUser1 = new User("fstep-user1");
        fstepUser1.setRole(Role.USER);
        fstepUser2 = new User("fstep-user2");
        fstepUser2.setRole(Role.USER);
        fstepUser3 = new User("fstep-user3");
        fstepUser4 = new User("fstep-user4");
        fstepUser2.setRole(Role.USER);
        fstepAdmin = new User("fstep-admin");
        fstepAdmin.setRole(Role.ADMIN);
        Quota quota2 = new Quota();
        quota2.setOwner(fstepUser2);
        quota2.setUsageType(UsageType.MAX_RUNNABLE_JOBS);
        quota2.setValue(15L);
        Quota quota4 = new Quota();
        quota4.setOwner(fstepUser4);
        quota4.setUsageType(UsageType.MAX_RUNNABLE_JOBS);
        quota4.setValue(30L);
        userDataService.save(ImmutableSet.of(fstepUser1, fstepUser2, fstepUser3, fstepUser4, fstepAdmin));
        dataService.save(ImmutableSet.of(quota2, quota4));
    }

    @After
    public void tearDown() {
        dataService.deleteAll();
    }

    @Test
    public void testGetUsageTypes() throws Exception {
       mockMvc.perform(get("/api/quotas/usageTypes").header("REMOTE_USER", fstepUser1.getName()))
        		.andExpect(status().isOk()).andExpect(jsonPath("$").isArray()).andExpect(jsonPath("$").value(contains(UsageType.MAX_RUNNABLE_JOBS.toString())));
    }
    
    @Test
    public void testGetUserDefaultValue() throws Exception {
    	mockMvc.perform(get("/api/quotas/value").param("usageType", "MAX_RUNNABLE_JOBS").header("REMOTE_USER", fstepUser1.getName()))
		.andExpect(status().isOk()).andExpect(jsonPath("$").isNumber()).andExpect(jsonPath("$").value(equalTo(5)));
        
    }
    
    @Test
    public void testGetUserOverriddenValue() throws Exception {
        mockMvc.perform(get("/api/quotas/value").param("usageType", "MAX_RUNNABLE_JOBS").header("REMOTE_USER", fstepUser2.getName()))
		.andExpect(status().isOk()).andExpect(jsonPath("$").isNumber()).andExpect(jsonPath("$").value(equalTo(15)));
        
    }
    
    @Test
    public void testSaveUserQuota() throws Exception {
    	mockMvc.perform(post("/api/quotas").content("{\"usageType\": \"MAX_RUNNABLE_JOBS\", \"value\": 12, \"owner\":\"" + userUri(fstepUser3) + "\"}").header("REMOTE_USER", fstepAdmin.getName()))
 		.andExpect(status().isCreated());
    	
    	 mockMvc.perform(get("/api/quotas/value").param("usageType", "MAX_RUNNABLE_JOBS").header("REMOTE_USER", fstepUser3.getName()))
 		.andExpect(status().isOk()).andExpect(jsonPath("$").isNumber()).andExpect(jsonPath("$").value(equalTo(12)));
    }
    
    private String userUri(User user) throws Exception {
        String jsonResult = mockMvc.perform(
                get("/api/users/" + user.getId()).header("REMOTE_USER", fstepAdmin.getName()))
                .andReturn().getResponse().getContentAsString();
        return USER_HREF_JSONPATH.read(jsonResult);
    }
    
    @Test
    public void testFindByOwner() throws Exception {
        String fstepUser2Url = JsonPath.compile("$._links.self.href")
                .read(mockMvc.perform(get("/api/users/" + fstepUser2.getId()).header("REMOTE_USER", fstepUser2.getName()))
                        .andReturn().getResponse().getContentAsString());

        mockMvc.perform(get("/api/quotas/search/findByOwner?owner=" + fstepUser2Url)
                .header("REMOTE_USER", fstepUser2.getName())).andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.quotas").isArray())
                .andExpect(jsonPath("$._embedded.quotas.length()").value(1));
        
        mockMvc.perform(get("/api/quotas/search/findByOwner?owner=" + fstepUser2Url)
                .header("REMOTE_USER", fstepAdmin.getName())).andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.quotas").isArray())
                .andExpect(jsonPath("$._embedded.quotas.length()").value(1));
        
        mockMvc.perform(get("/api/quotas/search/findByOwner?owner=" + fstepUser2Url)
                .header("REMOTE_USER", fstepUser4.getName())).andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.quotas").isArray())
                .andExpect(jsonPath("$._embedded.quotas.length()").value(0));
    }
    
    @Test
    public void testFindByOwnerAndUsageType() throws Exception {
    	String fstepUser2Url = JsonPath.compile("$._links.self.href")
                .read(mockMvc.perform(get("/api/users/" + fstepUser2.getId()).header("REMOTE_USER", fstepUser2.getName()))
                        .andReturn().getResponse().getContentAsString());
    	mockMvc.perform(get("/api/quotas/search/findByUsageTypeAndOwner?usageType=MAX_RUNNABLE_JOBS&owner=" + fstepUser2Url)
            .header("REMOTE_USER", fstepAdmin.getName())).andExpect(status().isOk())
            .andExpect(jsonPath("$.value").value(15));
            
    }


}
