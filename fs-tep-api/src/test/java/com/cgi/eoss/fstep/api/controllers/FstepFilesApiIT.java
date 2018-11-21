package com.cgi.eoss.fstep.api.controllers;

import com.cgi.eoss.fstep.api.ApiConfig;
import com.cgi.eoss.fstep.api.ApiTestConfig;
import com.cgi.eoss.fstep.catalogue.CatalogueService;
import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.Role;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.persistence.service.FstepFileDataService;
import com.cgi.eoss.fstep.persistence.service.UserDataService;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteStreams;
import com.jayway.jsonpath.JsonPath;
import okhttp3.HttpUrl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.fileUpload;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {ApiConfig.class, ApiTestConfig.class})
@AutoConfigureMockMvc
@TestPropertySource("classpath:test-api.properties")
@Transactional
public class FstepFilesApiIT {

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private FstepFileDataService fileDataService;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CatalogueService catalogueService;

    private FstepFile testFile1;
    private FstepFile testFile2;

    private User fstepUser;
    private User fstepAdmin;

    @Before
    public void setUp() throws Exception {
        fstepUser = new User("fstep-user");
        fstepUser.setRole(Role.USER);
        fstepAdmin = new User("fstep-admin");
        fstepAdmin.setRole(Role.ADMIN);
        userDataService.save(ImmutableSet.of(fstepUser, fstepAdmin));

        UUID fileUuid = UUID.randomUUID();
        testFile1 = new FstepFile(URI.create("fstep://refData/2/testFile1"), fileUuid);
        testFile1.setOwner(fstepAdmin);
        testFile1.setFilename("testFile1");
        testFile1.setType(FstepFile.Type.REFERENCE_DATA);

        UUID file2Uuid = UUID.randomUUID();
        testFile2 = new FstepFile(URI.create("fstep://outputProduct/job1/testFile2"), file2Uuid);
        testFile2.setOwner(fstepAdmin);
        testFile2.setFilename("testFile2");
        testFile2.setType(FstepFile.Type.OUTPUT_PRODUCT);

        fileDataService.save(ImmutableSet.of(testFile1, testFile2));
    }

    @After
    public void tearDown() throws Exception {
        fileDataService.deleteAll();
    }

    @Test
    public void testGet() throws Exception {
        mockMvc.perform(get("/api/fstepFiles/" + testFile1.getId()).header("REMOTE_USER", fstepAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href").value(endsWith("/fstepFiles/" + testFile1.getId())))
                .andExpect(jsonPath("$._links.download.href").value(endsWith("/fstepFiles/" + testFile1.getId() + "/dl")));

        mockMvc.perform(get("/api/fstepFiles/search/findByType?type=REFERENCE_DATA").header("REMOTE_USER", fstepAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.fstepFiles").isArray())
                .andExpect(jsonPath("$._embedded.fstepFiles.length()").value(1))
                .andExpect(jsonPath("$._embedded.fstepFiles[0].filename").value("testFile1"));

        // Results are filtered by ACL
        mockMvc.perform(get("/api/fstepFiles/search/findByType?type=REFERENCE_DATA").header("REMOTE_USER", fstepUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.fstepFiles").isArray())
                .andExpect(jsonPath("$._embedded.fstepFiles.length()").value(0));

        mockMvc.perform(get("/api/fstepFiles/search/findByType?type=OUTPUT_PRODUCT").header("REMOTE_USER", fstepAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.fstepFiles").isArray())
                .andExpect(jsonPath("$._embedded.fstepFiles.length()").value(1))
                .andExpect(jsonPath("$._embedded.fstepFiles[0].filename").value("testFile2"));
    }

    @Test
    public void testGetWithProjection() throws Exception {
        when(catalogueService.getWmsUrl(testFile1.getType(), testFile1.getUri())).thenReturn(HttpUrl.parse("http://example.com/wms"));

        mockMvc.perform(get("/api/fstepFiles/" + testFile1.getId() + "?projection=detailedFstepFile").header("REMOTE_USER", fstepAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href").value(endsWith("/fstepFiles/" + testFile1.getId())))
                .andExpect(jsonPath("$._links.download.href").value(endsWith("/fstepFiles/" + testFile1.getId() + "/dl")))
                .andExpect(jsonPath("$._links.wms.href").value("http://example.com/wms"))
                .andExpect(jsonPath("$._links.fstep.href").value(testFile1.getUri().toASCIIString()));

        mockMvc.perform(get("/api/fstepFiles/" + testFile1.getId() + "?projection=shortFstepFile").header("REMOTE_USER", fstepAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href").value(endsWith("/fstepFiles/" + testFile1.getId())))
                .andExpect(jsonPath("$._links.download.href").value(endsWith("/fstepFiles/" + testFile1.getId() + "/dl")))
                .andExpect(jsonPath("$._links.wms").doesNotExist())
                .andExpect(jsonPath("$._links.fstep").doesNotExist());
    }

    @Test
    public void testFindByOwner() throws Exception {
        String fstepUserUrl = JsonPath.compile("$._links.self.href").read(
                mockMvc.perform(get("/api/users/" + fstepUser.getId()).header("REMOTE_USER", fstepAdmin.getName())).andReturn().getResponse().getContentAsString()
        );
        String fstepAdminUrl = JsonPath.compile("$._links.self.href").read(
                mockMvc.perform(get("/api/users/" + fstepAdmin.getId()).header("REMOTE_USER", fstepAdmin.getName())).andReturn().getResponse().getContentAsString()
        );

        mockMvc.perform(get("/api/fstepFiles/search/findByOwner?owner="+fstepUserUrl).header("REMOTE_USER", fstepAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.fstepFiles").isArray())
                .andExpect(jsonPath("$._embedded.fstepFiles.length()").value(0));

        mockMvc.perform(get("/api/fstepFiles/search/findByOwner?owner="+fstepAdminUrl).header("REMOTE_USER", fstepAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.fstepFiles").isArray())
                .andExpect(jsonPath("$._embedded.fstepFiles.length()").value(2));
    }

    @Test
    public void testFindByNotOwner() throws Exception {
        String fstepUserUrl = JsonPath.compile("$._links.self.href").read(
                mockMvc.perform(get("/api/users/" + fstepUser.getId()).header("REMOTE_USER", fstepAdmin.getName())).andReturn().getResponse().getContentAsString()
        );
        String fstepAdminUrl = JsonPath.compile("$._links.self.href").read(
                mockMvc.perform(get("/api/users/" + fstepAdmin.getId()).header("REMOTE_USER", fstepAdmin.getName())).andReturn().getResponse().getContentAsString()
        );

        mockMvc.perform(get("/api/fstepFiles/search/findByNotOwner?owner="+fstepUserUrl).header("REMOTE_USER", fstepAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.fstepFiles").isArray())
                .andExpect(jsonPath("$._embedded.fstepFiles.length()").value(2));

        mockMvc.perform(get("/api/fstepFiles/search/findByNotOwner?owner="+fstepAdminUrl).header("REMOTE_USER", fstepAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.fstepFiles").isArray())
                .andExpect(jsonPath("$._embedded.fstepFiles.length()").value(0));
    }

    @Test
    public void testSaveRefData() throws Exception {
        Resource fileResource = new ClassPathResource("/testFile1", FstepFilesApiIT.class);
        MockMultipartFile uploadFile = new MockMultipartFile("file", "testFile1", "text/plain", fileResource.getInputStream());

        when(catalogueService.ingestReferenceData(any(), any())).thenReturn(testFile1);
        mockMvc.perform(fileUpload("/api/fstepFiles/refData").file(uploadFile).header("REMOTE_USER", fstepUser.getName()).param("fileType", "OTHER"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$._links.self.href").value(endsWith("/fstepFiles/" + testFile1.getId())))
                .andExpect(jsonPath("$._links.download.href").value(endsWith("/fstepFiles/" + testFile1.getId() + "/dl")));
        verify(catalogueService).ingestReferenceData(any(), any());
    }

    @Test
    public void testDownloadFile() throws Exception {
        Resource response = new ClassPathResource("/testFile1", FstepFilesApiIT.class);
        when(catalogueService.getAsResource(testFile1)).thenReturn(response);

        mockMvc.perform(get("/api/fstepFiles/" + testFile1.getId() + "/dl").header("REMOTE_USER", fstepUser.getName()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/fstepFiles/" + testFile1.getId() + "/dl").header("REMOTE_USER", fstepAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"testFile1\""))
                .andExpect(content().bytes(ByteStreams.toByteArray(response.getInputStream())));
    }

}