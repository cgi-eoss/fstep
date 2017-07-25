package com.cgi.eoss.fstep.api.controllers;

import com.cgi.eoss.fstep.api.ApiConfig;
import com.cgi.eoss.fstep.api.ApiTestConfig;
import com.cgi.eoss.fstep.model.Databasket;
import com.cgi.eoss.fstep.model.Role;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.persistence.service.DatabasketDataService;
import com.cgi.eoss.fstep.persistence.service.UserDataService;
import com.google.common.collect.ImmutableSet;
import com.jayway.jsonpath.JsonPath;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {ApiConfig.class, ApiTestConfig.class})
@AutoConfigureMockMvc
@TestPropertySource("classpath:test-api.properties")
@Transactional
public class DatabasketsApiIT {

    @Autowired
    private DatabasketDataService dataService;

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private MockMvc mockMvc;

    private User fstepUser;
    private User fstepAdmin;

    private Databasket databasket1;
    private Databasket databasket2;
    private Databasket databasket3;

    @Before
    public void setUp() {
        fstepUser = new User("fstep-user");
        fstepUser.setRole(Role.USER);
        fstepAdmin = new User("fstep-admin");
        fstepAdmin.setRole(Role.ADMIN);

        userDataService.save(ImmutableSet.of(fstepUser, fstepAdmin));

        databasket1 = new Databasket("Basket1", fstepUser);
        databasket1.setDescription("test basket 1");
        databasket2 = new Databasket("Basket2", fstepUser);
        databasket2.setDescription("test basket 2");
        databasket3 = new Databasket("Basket3", fstepUser);
        databasket3.setDescription("my data collection");

        dataService.save(ImmutableSet.of(databasket1, databasket2, databasket3));
    }

    @After
    public void tearDown() {
        dataService.deleteAll();
    }

    @Test
    public void testFindByFilterOnly() throws Exception {
        mockMvc.perform(get("/api/databaskets/search/findByFilterOnly?filter=Basket1").header("REMOTE_USER",
                fstepUser.getName())).andExpect(status().isOk()).andExpect(jsonPath("$._embedded.databaskets").isArray())
                .andExpect(jsonPath("$._embedded.databaskets.length()").value(1))
                .andExpect(jsonPath("$._embedded.databaskets[0].name").value("Basket1"));

        mockMvc.perform(
                get("/api/databaskets/search/findByFilterOnly?filter=Basket").header("REMOTE_USER", fstepUser.getName()))
                .andExpect(status().isOk()).andExpect(jsonPath("$._embedded.databaskets").isArray())
                .andExpect(jsonPath("$._embedded.databaskets.length()").value(3));

        mockMvc.perform(get("/api/databaskets/search/findByFilterOnly?filter=Basket4").header("REMOTE_USER",
                fstepUser.getName())).andExpect(status().isOk()).andExpect(jsonPath("$._embedded.databaskets").isArray())
                .andExpect(jsonPath("$._embedded.databaskets.length()").value(0));

        mockMvc.perform(
                get("/api/databaskets/search/findByFilterOnly?filter=basket").header("REMOTE_USER", fstepUser.getName()))
                .andExpect(status().isOk()).andExpect(jsonPath("$._embedded.databaskets").isArray())
                .andExpect(jsonPath("$._embedded.databaskets.length()").value(3));

        mockMvc.perform(
                get("/api/databaskets/search/findByFilterOnly?filter=BASKET").header("REMOTE_USER", fstepUser.getName()))
                .andExpect(status().isOk()).andExpect(jsonPath("$._embedded.databaskets").isArray())
                .andExpect(jsonPath("$._embedded.databaskets.length()").value(3));

        mockMvc.perform(
                get("/api/databaskets/search/findByFilterOnly?filter=Test").header("REMOTE_USER", fstepUser.getName()))
                .andExpect(status().isOk()).andExpect(jsonPath("$._embedded.databaskets").isArray())
                .andExpect(jsonPath("$._embedded.databaskets.length()").value(2));

        mockMvc.perform(
                get("/api/databaskets/search/findByFilterOnly?filter=coll").header("REMOTE_USER", fstepUser.getName()))
                .andExpect(status().isOk()).andExpect(jsonPath("$._embedded.databaskets").isArray())
                .andExpect(jsonPath("$._embedded.databaskets.length()").value(1))
                .andExpect(jsonPath("$._embedded.databaskets[0].name").value("Basket3"));

        mockMvc.perform(
                get("/api/databaskets/search/findByFilterOnly?filter=sket").header("REMOTE_USER", fstepUser.getName()))
                .andExpect(status().isOk()).andExpect(jsonPath("$._embedded.databaskets").isArray())
                .andExpect(jsonPath("$._embedded.databaskets.length()").value(3));
    }

    @Test
    public void testFindByFilterAndOwner() throws Exception {
        String fstepUserUrl = JsonPath.compile("$._links.self.href")
                .read(mockMvc.perform(get("/api/users/" + fstepUser.getId()).header("REMOTE_USER", fstepUser.getName()))
                        .andReturn().getResponse().getContentAsString());

        mockMvc.perform(get("/api/databaskets/search/findByFilterAndOwner?filter=SKet1&owner=" + fstepUserUrl)
                .header("REMOTE_USER", fstepUser.getName())).andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.databaskets").isArray())
                .andExpect(jsonPath("$._embedded.databaskets.length()").value(1))
                .andExpect(jsonPath("$._embedded.databaskets[0].name").value("Basket1"));
    }

    @Test
    public void testFindByFilterAndNotOwner() throws Exception {
        String fstepUserUrl = JsonPath.compile("$._links.self.href")
                .read(mockMvc.perform(get("/api/users/" + fstepUser.getId()).header("REMOTE_USER", fstepUser.getName()))
                        .andReturn().getResponse().getContentAsString());

        String fstepAdminUrl = JsonPath.compile("$._links.self.href")
                .read(mockMvc.perform(get("/api/users/" + fstepAdmin.getId()).header("REMOTE_USER", fstepAdmin.getName()))
                        .andReturn().getResponse().getContentAsString());

        mockMvc.perform(get("/api/databaskets/search/findByFilterAndNotOwner?filter=Basket1&owner=" + fstepUserUrl)
                .header("REMOTE_USER", fstepUser.getName())).andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.databaskets").isArray())
                .andExpect(jsonPath("$._embedded.databaskets.length()").value(0));

        mockMvc.perform(get("/api/databaskets/search/findByFilterAndNotOwner?filter=&owner=" + fstepAdminUrl)
                .header("REMOTE_USER", fstepAdmin.getName())).andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.databaskets").isArray())
                .andExpect(jsonPath("$._embedded.databaskets.length()").value(3));
    }

}
