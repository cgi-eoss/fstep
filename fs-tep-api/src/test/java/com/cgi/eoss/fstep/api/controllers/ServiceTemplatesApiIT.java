package com.cgi.eoss.fstep.api.controllers;

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

import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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

import com.cgi.eoss.fstep.api.ApiConfig;
import com.cgi.eoss.fstep.api.ApiTestConfig;
import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.FstepServiceDescriptor;
import com.cgi.eoss.fstep.model.FstepServiceDescriptor.Parameter.DataNodeType;
import com.cgi.eoss.fstep.model.FstepServiceTemplate;
import com.cgi.eoss.fstep.model.Role;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.persistence.service.ServiceDataService;
import com.cgi.eoss.fstep.persistence.service.ServiceTemplateDataService;
import com.cgi.eoss.fstep.persistence.service.UserDataService;
import com.cgi.eoss.fstep.security.FstepPermission;
import com.cgi.eoss.fstep.security.FstepSecurityService;
import com.google.common.collect.ImmutableSet;
import com.jayway.jsonpath.JsonPath;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {ApiConfig.class, ApiTestConfig.class})
@AutoConfigureMockMvc
@TestPropertySource("classpath:test-api.properties")
@Transactional
public class ServiceTemplatesApiIT {

    private static final JsonPath USER_HREF_JSONPATH = JsonPath.compile("$._links.self.href");
    private static final JsonPath OBJ_ID_JSONPATH = JsonPath.compile("$.id");

    @Autowired
    private ServiceTemplateDataService dataService;

    @Autowired
    private ServiceDataService serviceDataService;

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
    private User fstepExpertUser2;
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
        fstepExpertUser2 = new User("fstep-expert-user2");
        fstepExpertUser2.setRole(Role.EXPERT_USER);
        fstepContentAuthority = new User("fstep-content-authority");
        fstepContentAuthority.setRole(Role.CONTENT_AUTHORITY);
        fstepAdmin = new User("fstep-admin");
        fstepAdmin.setRole(Role.ADMIN);

        userDataService.save(ImmutableSet.of(fstepGuest, fstepUser, fstepExpertUser, fstepExpertUser2, fstepContentAuthority, fstepAdmin));
    }

    @After
    public void tearDown() {
        dataService.deleteAll();
    }

    @Test
    public void testGetIndex() throws Exception {
        mockMvc.perform(get("/api/").header("REMOTE_USER", fstepUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.serviceTemplates").exists());
    }

    @Test
    public void testGet() throws Exception {
        FstepServiceTemplate serviceTemplate = new FstepServiceTemplate("service-template-1", fstepUser);
        serviceTemplate.setDescription("service-template-desc-1");
        dataService.save(serviceTemplate);

        mockMvc.perform(get("/api/serviceTemplates").header("REMOTE_USER", fstepUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.serviceTemplates").isArray())
                .andExpect(jsonPath("$._embedded.serviceTemplates[0].id").value(serviceTemplate.getId()))
                .andExpect(jsonPath("$._embedded.serviceTemplates[0].name").value("service-template-1"))
                .andExpect(jsonPath("$._embedded.serviceTemplates[0].owner.id").value(fstepUser.getId()))
                .andExpect(jsonPath("$._embedded.serviceTemplates[0].access.published").value(false))
                .andExpect(jsonPath("$._embedded.serviceTemplates[0].access.publishRequested").value(false))
                .andExpect(jsonPath("$._embedded.serviceTemplates[0].access.currentLevel").value("ADMIN"))
                .andExpect(jsonPath("$._embedded.serviceTemplates[0]._links.self.href").value(endsWith("/serviceTemplates/" + serviceTemplate.getId())))
                .andExpect(jsonPath("$._embedded.serviceTemplates[0]._links.owner.href").value(endsWith("/serviceTemplates/" + serviceTemplate.getId() + "/owner")));
    }

    @Test
    public void testGetFilter() throws Exception {
        FstepServiceTemplate serviceTemplate = new FstepServiceTemplate("service-template-1", fstepAdmin);
        FstepServiceTemplate serviceTemplate2 = new FstepServiceTemplate("service-template-2", fstepAdmin);
        FstepServiceTemplate serviceTemplate3 = new FstepServiceTemplate("service-template-3", fstepAdmin);
        dataService.save(ImmutableSet.of(serviceTemplate, serviceTemplate2, serviceTemplate3));

        createAce(new ObjectIdentityImpl(FstepServiceTemplate.class, serviceTemplate.getId()), new GrantedAuthoritySid(FstepPermission.PUBLIC), BasePermission.READ);
        createReadAce(new ObjectIdentityImpl(FstepServiceTemplate.class, serviceTemplate3.getId()), fstepExpertUser.getName());

        // service1 is returned as it is PUBLIC
        // service2 is not returned as it is not readable by the user
        // service3 is returned as the user has been granted read permission

        mockMvc.perform(get("/api/serviceTemplates").header("REMOTE_USER", fstepExpertUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.serviceTemplates").isArray())
                .andExpect(jsonPath("$._embedded.serviceTemplates.length()").value(2))
                .andExpect(jsonPath("$._embedded.serviceTemplates[0].id").value(serviceTemplate.getId()))
                .andExpect(jsonPath("$._embedded.serviceTemplates[0].access.published").value(true))
                .andExpect(jsonPath("$._embedded.serviceTemplates[0].access.currentLevel").value("READ"))
                .andExpect(jsonPath("$._embedded.serviceTemplates[1].id").value(serviceTemplate3.getId()))
                .andExpect(jsonPath("$._embedded.serviceTemplates[1].access.published").value(false))
                .andExpect(jsonPath("$._embedded.serviceTemplates[1].access.currentLevel").value("READ"));
    }

    @Test
    public void testCreateWithValidRole() throws Exception {
        mockMvc.perform(post("/api/serviceTemplates").header("REMOTE_USER", fstepAdmin.getName()).content("{\"name\": \"service-template-1\", \"owner\":\"" + userUri(fstepAdmin) + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/serviceTemplates/\\d+$")));
        mockMvc.perform(post("/api/serviceTemplates").header("REMOTE_USER", fstepContentAuthority.getName()).content("{\"name\": \"service-template-2\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/serviceTemplates/\\d+$")));

        assertThat(dataService.getByName("service-template-1"), is(notNullValue()));
        assertThat(dataService.getByName("service-template-1").getOwner(), is(fstepAdmin));
        assertThat(dataService.getByName("service-template-2"), is(notNullValue()));
        assertThat(dataService.getByName("service-template-2").getOwner(), is(fstepContentAuthority));
    }
    
    @Test
    public void testCreateServiceFromTemplate() throws Exception {
    	FstepServiceTemplate serviceTemplate = new FstepServiceTemplate("service-template-1", fstepExpertUser);
    	FstepServiceDescriptor serviceDescriptor = new FstepServiceDescriptor();
    	serviceDescriptor.setDataInputs(Collections.singletonList(new FstepServiceDescriptor.Parameter("templateInput", "The template input", "Dec", 1, 1, DataNodeType.LITERAL, null, null, null)));
    	serviceDescriptor.setDataOutputs(Collections.emptyList());
    	serviceTemplate.setServiceDescriptor(serviceDescriptor);
        dataService.save(serviceTemplate);
        createAce(new ObjectIdentityImpl(FstepServiceTemplate.class, serviceTemplate.getId()), new GrantedAuthoritySid(FstepPermission.PUBLIC), BasePermission.READ);
        mockMvc.perform(post("/api/serviceTemplates/" +serviceTemplate.getId() +  "/newService").header("REMOTE_USER", fstepExpertUser2.getName()).contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"service-1\", \"dockerTag\": \"dockerTag\"}"))
                .andExpect(status().isCreated());
        FstepService service = serviceDataService.getByName("service-1");
        assertThat(service, is(notNullValue()));
        assertThat(service.getOwner(), is(fstepExpertUser2));
        assertThat(service.getServiceDescriptor().getDataInputs().get(0).getId(), is("templateInput"));
    }

    @Test
    public void testCreateWithInvalidRole() throws Exception {
        mockMvc.perform(post("/api/serviceTemplates").header("REMOTE_USER", fstepUser.getName()).content("{\"name\": \"service-template-1\", \"owner\":\"" + userUri(fstepUser) + "\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testWriteAccessControl() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/serviceTemplates").header("REMOTE_USER", fstepAdmin.getName()).content("{\"name\": \"service-template-1\", \"owner\":\"" + userUri(fstepAdmin) + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/serviceTemplates/\\d+$")))
                .andReturn();

        String serviceTemplateLocation = result.getResponse().getHeader("Location");

        // WARNING: The underlying object *is* modified by these calls, due to ORM state held in the mockMvc layer
        // This should not happen in production and must be verified in the full test harness

        mockMvc.perform(patch(serviceTemplateLocation).header("REMOTE_USER", fstepUser.getName()).content("{\"name\": \"service-template-1-user-updated\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch(serviceTemplateLocation).header("REMOTE_USER", fstepGuest.getName()).content("{\"name\": \"service-template-1-guest-updated\"}"))
                .andExpect(status().isForbidden());

        // Allow the user to write to the object
        createWriteAce(new ObjectIdentityImpl(FstepServiceTemplate.class, getJsonObjectId(serviceTemplateLocation)), fstepUser.getName());

        mockMvc.perform(patch(serviceTemplateLocation).header("REMOTE_USER", fstepUser.getName()).content("{\"name\": \"service-template-1-user-updated\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(patch(serviceTemplateLocation).header("REMOTE_USER", fstepGuest.getName()).content("{\"name\": \"service-template-1-guest-updated\"}"))
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