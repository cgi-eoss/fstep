package com.cgi.eoss.fstep.api.controllers;

import com.cgi.eoss.fstep.api.ApiConfig;
import com.cgi.eoss.fstep.api.ApiTestConfig;
import com.cgi.eoss.fstep.model.Role;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.persistence.service.UserDataService;
import com.google.common.collect.ImmutableSet;
import com.jayway.jsonpath.JsonPath;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {ApiConfig.class, ApiTestConfig.class})
@AutoConfigureMockMvc
@TestPropertySource("classpath:test-api.properties")
@Transactional
public class WalletsApiIT {
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
    public void testGetAll() throws Exception {
        mockMvc.perform(get("/api/wallets").header("REMOTE_USER", fstepUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.wallets.length()").value(1))
                .andExpect(jsonPath("$._embedded.wallets[0].owner.name").value("fstep-user"));

        mockMvc.perform(get("/api/wallets").header("REMOTE_USER", fstepGuest.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.wallets.length()").value(1))
                .andExpect(jsonPath("$._embedded.wallets[0].owner.name").value("fstep-guest"));

        mockMvc.perform(get("/api/wallets").header("REMOTE_USER", fstepAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.wallets.length()").value(3));
    }

    @Test
    public void testGet() throws Exception {
        String walletUrl = getWalletUrl(fstepUser);

        mockMvc.perform(get(walletUrl).header("REMOTE_USER", fstepGuest.getName()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(walletUrl).header("REMOTE_USER", fstepAdmin.getName()))
                .andExpect(status().isOk());

        String walletJson = mockMvc.perform(get(walletUrl).header("REMOTE_USER", fstepUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(100))
                .andReturn().getResponse().getContentAsString();

        String transactionsUrl = ((String) JsonPath.compile("$._links.transactions.href").read(walletJson)).replace("{?projection}", "");

        mockMvc.perform(get(transactionsUrl).header("REMOTE_USER", fstepGuest.getName()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(transactionsUrl).header("REMOTE_USER", fstepAdmin.getName()))
                .andExpect(status().isOk());

        mockMvc.perform(get(transactionsUrl).header("REMOTE_USER", fstepUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.walletTransactions").isArray())
                .andExpect(jsonPath("$._embedded.walletTransactions.length()").value(1))
                .andExpect(jsonPath("$._embedded.walletTransactions[0].balanceChange").value(100));
    }

    @Test
    public void testCredit() throws Exception {
        String userWalletUrl = getWalletUrl(fstepUser);

        mockMvc.perform(get(userWalletUrl).header("REMOTE_USER", fstepUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(100));

        mockMvc.perform(post(userWalletUrl + "/credit").contentType(MediaType.APPLICATION_JSON).content("{\"amount\":50}").header("REMOTE_USER", fstepUser.getName()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(userWalletUrl).header("REMOTE_USER", fstepUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(100));

        mockMvc.perform(post(userWalletUrl + "/credit").contentType(MediaType.APPLICATION_JSON).content("{\"amount\":50}").header("REMOTE_USER", fstepAdmin.getName()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get(userWalletUrl).header("REMOTE_USER", fstepUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(150));
    }

    private String getWalletUrl(User user) throws Exception {
        return ((String) JsonPath.compile("$._links.wallet.href").read(
                mockMvc.perform(get("/api/users/" + user.getId()).header("REMOTE_USER", fstepUser.getName()))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString())
        ).replace("{?projection}", "");
    }

}
