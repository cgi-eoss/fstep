package com.cgi.eoss.fstep.api.controllers;

import com.cgi.eoss.fstep.api.ApiConfig;
import com.cgi.eoss.fstep.api.ApiTestConfig;
import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.Role;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.persistence.service.ServiceDataService;
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

import static org.hamcrest.CoreMatchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {ApiConfig.class, ApiTestConfig.class})
@AutoConfigureMockMvc
@TestPropertySource("classpath:test-api.properties")
@Transactional
public class PublishingRequestsApiIT {
    @Autowired
    private UserDataService userDataService;

    @Autowired
    private ServiceDataService serviceDataService;

    private FstepService service1;
    private FstepService service2;
    private FstepService service3;

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

        userDataService.save(ImmutableSet.of(fstepGuest, fstepUser, fstepAdmin));

        service1 = new FstepService("service-1", fstepAdmin, "dockerTag");
        service1.setStatus(FstepService.Status.AVAILABLE);
        service2 = new FstepService("service-2", fstepUser, "dockerTag");
        service2.setStatus(FstepService.Status.IN_DEVELOPMENT);
        service3 = new FstepService("service-3", fstepGuest, "dockerTag");
        service3.setStatus(FstepService.Status.IN_DEVELOPMENT);
        serviceDataService.save(ImmutableSet.of(service1, service2, service3));
    }

    @Test
    public void testRequestPublishService() throws Exception {
        mockMvc.perform(post("/api/publishingRequests/requestPublishService/" + service1.getId()).header("REMOTE_USER", fstepUser.getName()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/publishingRequests/requestPublishService/" + service2.getId()).header("REMOTE_USER", fstepUser.getName()))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/publishingRequests/requestPublishService/" + service3.getId()).header("REMOTE_USER", fstepUser.getName()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/publishingRequests/requestPublishService/" + service2.getId()).header("REMOTE_USER", fstepUser.getName()))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testGet() throws Exception {
        String svc2Url = mockMvc.perform(post("/api/publishingRequests/requestPublishService/" + service2.getId()).header("REMOTE_USER", fstepUser.getName()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        String svc3Url = mockMvc.perform(post("/api/publishingRequests/requestPublishService/" + service3.getId()).header("REMOTE_USER", fstepGuest.getName()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(get(svc2Url).header("REMOTE_USER", fstepUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$._links.associated.href").value(endsWith("/api/services/" + service2.getId() + "{?projection}")));
        mockMvc.perform(get(svc2Url).header("REMOTE_USER", fstepGuest.getName()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(svc2Url).header("REMOTE_USER", fstepAdmin.getName()))
                .andExpect(status().isOk());

        mockMvc.perform(get(svc3Url).header("REMOTE_USER", fstepGuest.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$._links.associated.href").value(endsWith("/api/services/" + service3.getId() + "{?projection}")));
        mockMvc.perform(get(svc3Url).header("REMOTE_USER", fstepUser.getName()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/publishingRequests").header("REMOTE_USER", fstepUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.publishingRequests.length()").value(1));
        mockMvc.perform(get("/api/publishingRequests").header("REMOTE_USER", fstepGuest.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.publishingRequests.length()").value(1));
        mockMvc.perform(get("/api/publishingRequests").header("REMOTE_USER", fstepAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.publishingRequests.length()").value(2));
    }

    @Test
    public void testFindByStatusAndPublish() throws Exception {
        String svc2Url = mockMvc.perform(post("/api/publishingRequests/requestPublishService/" + service2.getId()).header("REMOTE_USER", fstepUser.getName()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        String svc3Url = mockMvc.perform(post("/api/publishingRequests/requestPublishService/" + service3.getId()).header("REMOTE_USER", fstepGuest.getName()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(get("/api/publishingRequests/search/findByStatus?status=REQUESTED").header("REMOTE_USER", fstepUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.publishingRequests.length()").value(1));
        mockMvc.perform(get("/api/publishingRequests/search/findByStatus?status=REQUESTED").header("REMOTE_USER", fstepGuest.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.publishingRequests.length()").value(1));
        mockMvc.perform(get("/api/publishingRequests/search/findByStatus?status=REQUESTED").header("REMOTE_USER", fstepAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.publishingRequests.length()").value(2));

        mockMvc.perform(post("/api/contentAuthority/services/publish/" + service2.getId()).header("REMOTE_USER", fstepAdmin.getName()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/publishingRequests/search/findByStatus?status=REQUESTED,NEEDS_INFO,REJECTED").header("REMOTE_USER", fstepUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.publishingRequests.length()").value(0));
        mockMvc.perform(get("/api/publishingRequests/search/findByStatus?status=GRANTED").header("REMOTE_USER", fstepUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.publishingRequests.length()").value(1));
        mockMvc.perform(get("/api/publishingRequests/search/findByStatus?status=REQUESTED").header("REMOTE_USER", fstepAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.publishingRequests.length()").value(1));
    }

}
