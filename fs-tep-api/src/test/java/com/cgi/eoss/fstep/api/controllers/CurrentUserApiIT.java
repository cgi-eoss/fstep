package com.cgi.eoss.fstep.api.controllers;

import com.cgi.eoss.fstep.api.ApiConfig;
import com.cgi.eoss.fstep.api.ApiTestConfig;
import com.cgi.eoss.fstep.model.Role;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.persistence.service.UserDataService;
import com.google.common.collect.ImmutableSet;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {ApiConfig.class, ApiTestConfig.class})
@AutoConfigureMockMvc
@TestPropertySource("classpath:test-api.properties")
@Transactional
public class CurrentUserApiIT {

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private MockMvc mockMvc;

    private User fstepUser;
    private User fstepAdmin;

    @Before
    public void setUp() {
        fstepUser = new User("fstep-user");
        fstepUser.setRole(Role.USER);
        fstepAdmin = new User("fstep-admin");
        fstepAdmin.setRole(Role.ADMIN);

        userDataService.save(ImmutableSet.of(fstepUser, fstepAdmin));
    }

    @Test
    public void currentUser() throws Exception {
        mockMvc.perform(get("/api/currentUser").header("REMOTE_USER", fstepUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(fstepUser.getId()))
                .andExpect(jsonPath("$.name").value(fstepUser.getName()));

        mockMvc.perform(get("/api/currentUser").header("REMOTE_USER", fstepAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(fstepAdmin.getId()))
                .andExpect(jsonPath("$.name").value(fstepAdmin.getName()));
    }

}