package com.cgi.eoss.fstep.api.controllers;

import com.cgi.eoss.fstep.api.ApiConfig;
import com.cgi.eoss.fstep.api.ApiTestConfig;
import com.cgi.eoss.fstep.security.FstepPermission;
import com.cgi.eoss.fstep.security.FstepSecurityService;
import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.Role;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.persistence.service.ServiceDataService;
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
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {ApiConfig.class, ApiTestConfig.class})
@AutoConfigureMockMvc
@TestPropertySource("classpath:test-api.properties")
@Transactional
public class ServicesApiIT {

    private static final JsonPath USER_HREF_JSONPATH = JsonPath.compile("$._links.self.href");
    private static final JsonPath OBJ_ID_JSONPATH = JsonPath.compile("$.id");

    @Autowired
    private ServiceDataService dataService;

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private MutableAclService aclService;

    @Autowired
    private FstepSecurityService securityService;

    @Autowired
    private MockMvc mockMvc;

    private User fstepGuest;
    private User fstepUser;
    private User fstepExpertUser;
    private User fstepContentAuthority;
    private User fstepAdmin;

    @Before
    public void setUp() {
        fstepGuest = new User("fstep-guest");
        fstepGuest.setRole(Role.GUEST);
        fstepUser = new User("fstep-user");
        fstepUser.setRole(Role.USER);
        fstepExpertUser = new User("fstep-expert-user");
        fstepExpertUser.setRole(Role.EXPERT_USER);
        fstepContentAuthority = new User("fstep-content-authority");
        fstepContentAuthority.setRole(Role.CONTENT_AUTHORITY);
        fstepAdmin = new User("fstep-admin");
        fstepAdmin.setRole(Role.ADMIN);

        userDataService.save(ImmutableSet.of(fstepGuest, fstepUser, fstepExpertUser, fstepContentAuthority, fstepAdmin));
    }

    @After
    public void tearDown() {
        dataService.deleteAll();
    }

    @Test
    public void testGetIndex() throws Exception {
        mockMvc.perform(get("/api/").header("REMOTE_USER", fstepUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.services").exists());
    }

    @Test
    public void testGet() throws Exception {
        FstepService service = new FstepService("service-1", fstepUser, "dockerTag");
        service.setStatus(FstepService.Status.AVAILABLE);
        dataService.save(service);

        mockMvc.perform(get("/api/services").header("REMOTE_USER", fstepUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.services").isArray())
                .andExpect(jsonPath("$._embedded.services[0].id").value(service.getId()))
                .andExpect(jsonPath("$._embedded.services[0].name").value("service-1"))
                .andExpect(jsonPath("$._embedded.services[0].dockerTag").value("dockerTag"))
                .andExpect(jsonPath("$._embedded.services[0].owner.id").value(fstepUser.getId()))
                .andExpect(jsonPath("$._embedded.services[0].access.published").value(false))
                .andExpect(jsonPath("$._embedded.services[0].access.publishRequested").value(false))
                .andExpect(jsonPath("$._embedded.services[0].access.currentLevel").value("ADMIN"))
                .andExpect(jsonPath("$._embedded.services[0]._links.self.href").value(endsWith("/services/" + service.getId())))
                .andExpect(jsonPath("$._embedded.services[0]._links.owner.href").value(endsWith("/services/" + service.getId() + "/owner")));
    }

    @Test
    public void testGetFilter() throws Exception {
        FstepService service = new FstepService("service-1", fstepAdmin, "dockerTag");
        service.setStatus(FstepService.Status.AVAILABLE);
        FstepService service2 = new FstepService("service-2", fstepAdmin, "dockerTag");
        service2.setStatus(FstepService.Status.IN_DEVELOPMENT);
        FstepService service3 = new FstepService("service-3", fstepAdmin, "dockerTag");
        service3.setStatus(FstepService.Status.IN_DEVELOPMENT);
        dataService.save(ImmutableSet.of(service, service2, service3));

        createAce(new ObjectIdentityImpl(FstepService.class, service.getId()), new GrantedAuthoritySid(FstepPermission.PUBLIC), BasePermission.READ);
        createReadAce(new ObjectIdentityImpl(FstepService.class, service3.getId()), fstepExpertUser.getName());

        // service1 is returned as it is AVAILABLE
        // service2 is not returned as it is IN_DEVELOPMENT and not readable by the user
        // service3 is returned as the user has been granted read permission

        mockMvc.perform(get("/api/services").header("REMOTE_USER", fstepExpertUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.services").isArray())
                .andExpect(jsonPath("$._embedded.services.length()").value(2))
                .andExpect(jsonPath("$._embedded.services[0].id").value(service.getId()))
                .andExpect(jsonPath("$._embedded.services[0].access.published").value(true))
                .andExpect(jsonPath("$._embedded.services[0].access.currentLevel").value("READ"))
                .andExpect(jsonPath("$._embedded.services[1].id").value(service3.getId()))
                .andExpect(jsonPath("$._embedded.services[1].access.published").value(false))
                .andExpect(jsonPath("$._embedded.services[1].access.currentLevel").value("READ"));
    }

    @Test
    public void testCreateWithValidRole() throws Exception {
        mockMvc.perform(post("/api/services").header("REMOTE_USER", fstepAdmin.getName()).content("{\"name\": \"service-1\", \"dockerTag\": \"dockerTag\", \"owner\":\"" + userUri(fstepAdmin) + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/services/\\d+$")));
        mockMvc.perform(post("/api/services").header("REMOTE_USER", fstepContentAuthority.getName()).content("{\"name\": \"service-2\", \"dockerTag\": \"dockerTag\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/services/\\d+$")));

        assertThat(dataService.getByName("service-1"), is(notNullValue()));
        assertThat(dataService.getByName("service-1").getOwner(), is(fstepAdmin));
        assertThat(dataService.getByName("service-2"), is(notNullValue()));
        assertThat(dataService.getByName("service-2").getOwner(), is(fstepContentAuthority));
    }

    @Test
    public void testCreateWithInvalidRole() throws Exception {
        mockMvc.perform(post("/api/services").header("REMOTE_USER", fstepUser.getName()).content("{\"name\": \"service-1\", \"dockerTag\": \"dockerTag\", \"owner\":\"" + userUri(fstepUser) + "\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testWriteAccessControl() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/services").header("REMOTE_USER", fstepAdmin.getName()).content("{\"name\": \"service-1\", \"dockerTag\": \"dockerTag\", \"owner\":\"" + userUri(fstepAdmin) + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/services/\\d+$")))
                .andReturn();

        String serviceLocation = result.getResponse().getHeader("Location");

        // WARNING: The underlying object *is* modified by these calls, due to ORM state held in the mockMvc layer
        // This should not happen in production and must be verified in the full test harness

        mockMvc.perform(patch(serviceLocation).header("REMOTE_USER", fstepUser.getName()).content("{\"name\": \"service-1-user-updated\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch(serviceLocation).header("REMOTE_USER", fstepGuest.getName()).content("{\"name\": \"service-1-guest-updated\"}"))
                .andExpect(status().isForbidden());

        // Allow the user to write to the object
        createWriteAce(new ObjectIdentityImpl(FstepService.class, getJsonObjectId(serviceLocation)), fstepUser.getName());

        mockMvc.perform(patch(serviceLocation).header("REMOTE_USER", fstepUser.getName()).content("{\"name\": \"service-1-user-updated\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(patch(serviceLocation).header("REMOTE_USER", fstepGuest.getName()).content("{\"name\": \"service-1-guest-updated\"}"))
                .andExpect(status().isForbidden());
    }

    private String userUri(User user) throws Exception {
        String jsonResult = mockMvc.perform(
                get("/api/users/" + user.getId()).header("REMOTE_USER", fstepAdmin.getName()))
                .andReturn().getResponse().getContentAsString();
        return USER_HREF_JSONPATH.read(jsonResult);
    }

    private Long getJsonObjectId(String location) throws Exception {
        String jsonResult = mockMvc.perform(get(location).header("REMOTE_USER", fstepAdmin.getName()))
                .andReturn().getResponse().getContentAsString();
        return ((Number) OBJ_ID_JSONPATH.read(jsonResult)).longValue();
    }

    private void createWriteAce(ObjectIdentity oi, String principal) {
        createReadAce(oi, principal);
        createAce(oi, new PrincipalSid(principal), BasePermission.WRITE);
    }

    private void createReadAce(ObjectIdentity oi, String principal) {
        createAce(oi, new PrincipalSid(principal), BasePermission.READ);
    }

    private void createAce(ObjectIdentity oi, Sid sid, Permission p) {
        SecurityContextHolder.getContext().setAuthentication(FstepSecurityService.PUBLIC_AUTHENTICATION);

        MutableAcl acl;
        try {
            acl = (MutableAcl) aclService.readAclById(oi);
        } catch (NotFoundException nfe) {
            acl = aclService.createAcl(oi);
        }

        acl.insertAce(acl.getEntries().size(), p, sid, true);
        aclService.updateAcl(acl);
    }

}