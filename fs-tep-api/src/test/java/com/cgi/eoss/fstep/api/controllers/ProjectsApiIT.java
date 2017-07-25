package com.cgi.eoss.fstep.api.controllers;

import com.cgi.eoss.fstep.api.ApiConfig;
import com.cgi.eoss.fstep.api.ApiTestConfig;
import com.cgi.eoss.fstep.model.Project;
import com.cgi.eoss.fstep.model.Role;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.persistence.service.ProjectDataService;
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
public class ProjectsApiIT {

    @Autowired
    private ProjectDataService dataService;

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private MockMvc mockMvc;

    private User fstepUser;
    private User fstepAdmin;

    private Project project1;
    private Project project2;
    private Project project3;

    @Before
    public void setUp() {
        fstepUser = new User("fstep-user");
        fstepUser.setRole(Role.USER);
        fstepAdmin = new User("fstep-admin");
        fstepAdmin.setRole(Role.ADMIN);

        userDataService.save(ImmutableSet.of(fstepUser, fstepAdmin));

        project1 = new Project("project 1", fstepUser);
        project1.setDescription("test project 1");
        project2 = new Project("project2", fstepUser);
        project2.setDescription("test project 2");
        project3 = new Project("project3", fstepUser);
        project3.setDescription("custom project");

        dataService.save(ImmutableSet.of(project1, project2, project3));
    }

    @After
    public void tearDown() {
        dataService.deleteAll();
    }

    @Test
    public void testFindByFilterOnly() throws Exception {
        mockMvc.perform(
                get("/api/projects/search/findByFilterOnly?filter=project2").header("REMOTE_USER", fstepUser.getName()))
                .andExpect(status().isOk()).andExpect(jsonPath("$._embedded.projects").isArray())
                .andExpect(jsonPath("$._embedded.projects.length()").value(1))
                .andExpect(jsonPath("$._embedded.projects[0].name").value("project2"));

        mockMvc.perform(
                get("/api/projects/search/findByFilterOnly?filter=test").header("REMOTE_USER", fstepUser.getName()))
                .andExpect(status().isOk()).andExpect(jsonPath("$._embedded.projects").isArray())
                .andExpect(jsonPath("$._embedded.projects.length()").value(2));

        mockMvc.perform(
                get("/api/projects/search/findByFilterOnly?filter=my").header("REMOTE_USER", fstepUser.getName()))
                .andExpect(status().isOk()).andExpect(jsonPath("$._embedded.projects").isArray())
                .andExpect(jsonPath("$._embedded.projects.length()").value(0));

        mockMvc.perform(
                get("/api/projects/search/findByFilterOnly?filter=custom").header("REMOTE_USER", fstepUser.getName()))
                .andExpect(status().isOk()).andExpect(jsonPath("$._embedded.projects").isArray())
                .andExpect(jsonPath("$._embedded.projects.length()").value(1))
                .andExpect(jsonPath("$._embedded.projects[0].name").value("project3"));

        mockMvc.perform(
                get("/api/projects/search/findByFilterOnly?filter=CUSTom").header("REMOTE_USER", fstepUser.getName()))
                .andExpect(status().isOk()).andExpect(jsonPath("$._embedded.projects").isArray())
                .andExpect(jsonPath("$._embedded.projects.length()").value(1))
                .andExpect(jsonPath("$._embedded.projects[0].name").value("project3"));

        mockMvc.perform(
                get("/api/projects/search/findByFilterOnly?filter=EST").header("REMOTE_USER", fstepUser.getName()))
                .andExpect(status().isOk()).andExpect(jsonPath("$._embedded.projects").isArray())
                .andExpect(jsonPath("$._embedded.projects.length()").value(2));

    }

    @Test
    public void testFindByFilterAndOwner() throws Exception {
        String fstepUserUrl = JsonPath.compile("$._links.self.href")
                .read(mockMvc.perform(get("/api/users/" + fstepUser.getId()).header("REMOTE_USER", fstepUser.getName()))
                        .andReturn().getResponse().getContentAsString());

        mockMvc.perform(get("/api/projects/search/findByFilterAndOwner?filter=EST&owner=" + fstepUserUrl)
                .header("REMOTE_USER", fstepUser.getName())).andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.projects").isArray())
                .andExpect(jsonPath("$._embedded.projects.length()").value(2));
    }

    @Test
    public void testFindByFilterAndNotOwner() throws Exception {
        String fstepUserUrl = JsonPath.compile("$._links.self.href")
                .read(mockMvc.perform(get("/api/users/" + fstepUser.getId()).header("REMOTE_USER", fstepUser.getName()))
                        .andReturn().getResponse().getContentAsString());

        String fstepAdminUrl = JsonPath.compile("$._links.self.href")
                .read(mockMvc.perform(get("/api/users/" + fstepAdmin.getId()).header("REMOTE_USER", fstepAdmin.getName()))
                        .andReturn().getResponse().getContentAsString());

        mockMvc.perform(get("/api/projects/search/findByFilterAndNotOwner?filter=esT&owner=" + fstepUserUrl)
                .header("REMOTE_USER", fstepUser.getName())).andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.projects").isArray())
                .andExpect(jsonPath("$._embedded.projects.length()").value(0));

        mockMvc.perform(get("/api/projects/search/findByFilterAndNotOwner?filter=&owner=" + fstepUserUrl)
                .header("REMOTE_USER", fstepUser.getName())).andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.projects").isArray())
                .andExpect(jsonPath("$._embedded.projects.length()").value(1))
                .andExpect(jsonPath("$._embedded.projects[0].name").value("Default Project"));

        mockMvc.perform(get("/api/projects/search/findByFilterAndNotOwner?filter=ES&owner=" + fstepAdminUrl)
                .header("REMOTE_USER", fstepAdmin.getName())).andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.projects").isArray())
                .andExpect(jsonPath("$._embedded.projects.length()").value(2));

        mockMvc.perform(get("/api/projects/search/findByFilterAndNotOwner?filter=&owner=" + fstepAdminUrl)
                .header("REMOTE_USER", fstepAdmin.getName())).andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.projects").isArray())
                .andExpect(jsonPath("$._embedded.projects.length()").value(4));
    }
}
