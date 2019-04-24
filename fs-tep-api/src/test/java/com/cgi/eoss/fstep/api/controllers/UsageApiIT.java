package com.cgi.eoss.fstep.api.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.util.UUID;

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
import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.Role;
import com.cgi.eoss.fstep.model.UsageType;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.persistence.service.FstepFileDataService;
import com.cgi.eoss.fstep.persistence.service.UserDataService;
import com.google.common.collect.ImmutableSet;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {ApiConfig.class, ApiTestConfig.class})
@AutoConfigureMockMvc
@TestPropertySource("classpath:test-api.properties")
@Transactional
public class UsageApiIT {
	
    @Autowired
    private UserDataService userDataService;
    
    @Autowired
    private FstepFileDataService fstepFileDataService;

    @Autowired
    private MockMvc mockMvc;

    private User fstepUser;

	private User fstepUser2;
    
    @Before
    public void setUp() {
        fstepUser = new User("fstep-user1");
        fstepUser.setRole(Role.USER);
        fstepUser2 = new User("fstep-user2");
        fstepUser2.setRole(Role.USER);
        FstepFile fstepFile = new FstepFile(URI.create("file:///test"), UUID.randomUUID());
        fstepFile.setOwner(fstepUser2);
        fstepFile.setFilesize(1_048_576L);
        userDataService.save(ImmutableSet.of(fstepUser, fstepUser2));
        fstepFileDataService.save(ImmutableSet.of(fstepFile));
    }

    @After
    public void tearDown() {
        
    }

    @Test
    public void testFindByUsageType() throws Exception {
        mockMvc.perform(get("/api/usage/value")
        		.param("usageType", UsageType.FILES_STORAGE_MB.getName())
                .header("REMOTE_USER", fstepUser.getName())).andExpect(status().isOk())
                .andExpect(jsonPath("$").value(0));
        mockMvc.perform(get("/api/usage/value")
        		.param("usageType", UsageType.FILES_STORAGE_MB.getName())
                .header("REMOTE_USER", fstepUser2.getName())).andExpect(status().isOk())
                .andExpect(jsonPath("$").value(1));
        
    }
    
    @Test
    public void testDirectFileStorageGetUsage() throws Exception {
        mockMvc.perform(get("/api/usage/files/storage")
        		.header("REMOTE_USER", fstepUser2.getName())).andExpect(status().isOk())
                .andExpect(jsonPath("$").value(1));
    }
}
