package com.cgi.eoss.fstep.api.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.cgi.eoss.fstep.api.ApiConfig;
import com.cgi.eoss.fstep.api.ApiTestConfig;
import com.cgi.eoss.fstep.model.Role;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.model.UserMount;
import com.cgi.eoss.fstep.model.UserMount.MountType;
import com.cgi.eoss.fstep.persistence.service.UserDataService;
import com.cgi.eoss.fstep.persistence.service.UserMountDataService;
import com.google.common.collect.ImmutableSet;
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

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {ApiConfig.class, ApiTestConfig.class})
@AutoConfigureMockMvc
@TestPropertySource("classpath:test-api.properties")
@Transactional
public class UserMountsApiIT {

    @Autowired
    private UserMountDataService dataService;

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private MockMvc mockMvc;

    private User fstepUser;
    private User fstepAdmin;

    private UserMount userMount1;
    private UserMount userMount2;
    private UserMount userMount3;

    @Before
    public void setUp() {
        fstepUser = new User("fstep-user");
        fstepUser.setRole(Role.USER);
        fstepAdmin = new User("fstep-admin");
        fstepAdmin.setRole(Role.ADMIN);

        userDataService.save(ImmutableSet.of(fstepUser, fstepAdmin));

        userMount1 = new UserMount("mount1", "/data/mount1", MountType.RO);

        userMount1.setOwner(fstepUser);

        userMount2 = new UserMount("mount2", "/data/mount2", MountType.RW);

        userMount2.setOwner(fstepUser);

        userMount3 = new UserMount("mount3", "/data/mount3", MountType.RO);

        userMount3.setOwner(fstepAdmin);

        dataService.save(ImmutableSet.of(userMount1, userMount2, userMount3));
    }

    @After
    public void tearDown() {
        dataService.deleteAll();
    }

    @Test
    public void testFindByType() throws Exception {
        mockMvc.perform(get("/api/userMounts/")
                .header("REMOTE_USER", fstepUser.getName())).andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.userMounts").isArray())
                .andExpect(jsonPath("$._embedded.userMounts.length()").value(2))
                .andExpect(jsonPath("$._embedded.userMounts[0].name").value("mount1"));

        mockMvc.perform(get("/api/userMounts/")
                .header("REMOTE_USER", "nonExistingUser")).andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.userMounts").isArray())
                .andExpect(jsonPath("$._embedded.userMounts.length()").value(0));

        mockMvc.perform(get("/api/userMounts/")
                .header("REMOTE_USER", fstepAdmin.getName())).andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.userMounts").isArray())
                .andExpect(jsonPath("$._embedded.userMounts.length()").value(3));

    }


}
