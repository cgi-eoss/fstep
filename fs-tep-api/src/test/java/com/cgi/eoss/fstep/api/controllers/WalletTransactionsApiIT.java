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
public class WalletTransactionsApiIT {
    @Autowired
    private UserDataService dataService;

    @Autowired
    private MockMvc mockMvc;

    private User fstepGuest;
    private User fstepUser;
    private User fstepAdmin;

    @Before
    public void setUp() {
        fstepGuest = new User("fstep-guest");
        fstepGuest.setRole(Role.GUEST);
        fstepUser = new User("fstep-user");
        fstepUser.setRole(Role.USER);
        fstepAdmin = new User("fstep-admin");
        fstepAdmin.setRole(Role.ADMIN);

        dataService.save(ImmutableSet.of(fstepGuest, fstepUser, fstepAdmin));
    }

    @Test
    public void testGet() throws Exception {
        mockMvc.perform(get("/api/walletTransactions").header("REMOTE_USER", fstepUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.walletTransactions").isArray())
                .andExpect(jsonPath("$._embedded.walletTransactions.length()").value(1))
                .andExpect(jsonPath("$._embedded.walletTransactions[0].balanceChange").value(100));

        mockMvc.perform(get("/api/walletTransactions").header("REMOTE_USER", fstepAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.walletTransactions").isArray())
                .andExpect(jsonPath("$._embedded.walletTransactions.length()").value(3));
    }

}
