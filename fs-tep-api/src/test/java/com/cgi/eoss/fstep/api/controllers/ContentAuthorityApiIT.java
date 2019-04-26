package com.cgi.eoss.fstep.api.controllers;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.cgi.eoss.fstep.api.ApiConfig;
import com.cgi.eoss.fstep.api.ApiTestConfig;
import com.cgi.eoss.fstep.model.Collection;
import com.cgi.eoss.fstep.model.CostingExpression;
import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.Role;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.orchestrator.zoo.ZooManagerClient;
import com.cgi.eoss.fstep.persistence.service.CollectionDataService;
import com.cgi.eoss.fstep.persistence.service.CostingExpressionDataService;
import com.cgi.eoss.fstep.persistence.service.ServiceDataService;
import com.cgi.eoss.fstep.persistence.service.ServiceFileDataService;
import com.cgi.eoss.fstep.persistence.service.UserDataService;
import com.cgi.eoss.fstep.services.DefaultFstepServices;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {ApiConfig.class, ApiTestConfig.class})
@AutoConfigureMockMvc
@TestPropertySource("classpath:test-api.properties")
@Transactional
public class ContentAuthorityApiIT {

    private static final String TEST_SERVICE_COSTING_EXPRESSION = "10";
    private static final String TEST_SERVICE_ESTIMATED_COSTING_EXPRESSION = "3";
    private static final String TEST_COLLECTION_COSTING_EXPRESSION = "5";
    
	private static final String TEST_COLLECTION_NAME = "fstep-collection";
	private static final String TEST_SERVICE_NAME = "SNAP";
    private static final int DEFAULT_SERVICE_COUNT = DefaultFstepServices.getDefaultServices().size();

    @MockBean
    private ZooManagerClient zooManagerClient;

    @Captor
    private ArgumentCaptor<List<FstepService>> argumentCaptor;

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private ServiceDataService serviceDataService;

    @Autowired
    private ServiceFileDataService serviceFileDataService;
    
    @Autowired
    private CollectionDataService collectionDataService;
    
    @MockBean 
    private CostingExpressionDataService costingExpressionDataService;

    @Captor
    private ArgumentCaptor<CostingExpression> costingExpressionCaptor;
    
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
        Collection collection = new Collection(TEST_COLLECTION_NAME, fstepUser);
        collection.setIdentifier("test");
        collectionDataService.save(collection);
    }

    @Test
    public void testRestoreDefaultServices() throws Exception {
        // Check nothing exists already
        assertThat(serviceDataService.getAll().size(), is(0));

        // Restore default services by HTTP POST
        mockMvc.perform(post("/api/contentAuthority/services/restoreDefaults").header("REMOTE_USER", fstepAdmin.getName()))
                .andExpect(status().isOk());

        assertThat(serviceDataService.getAll().size(), is(DEFAULT_SERVICE_COUNT));

        // Remove one of the default services
        FstepService testService = serviceDataService.getByName(TEST_SERVICE_NAME);
        serviceDataService.delete(testService);
        assertThat(serviceDataService.getByName(TEST_SERVICE_NAME), is(nullValue()));
        assertThat(serviceFileDataService.findByService(testService), is(empty()));

        // Restore default services again
        mockMvc.perform(post("/api/contentAuthority/services/restoreDefaults").header("REMOTE_USER", fstepAdmin.getName()))
                .andExpect(status().isOk());

        // Assert the deleted service has been recovered
        assertThat(serviceDataService.getAll().size(), is(DEFAULT_SERVICE_COUNT));
        assertThat(serviceDataService.getByName(TEST_SERVICE_NAME), is(notNullValue()));

        // Assert the default services are visible to the public
        mockMvc.perform(get("/api/services").header("REMOTE_USER", fstepUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.services").isArray())
                .andExpect(jsonPath("$._embedded.services.length()").value(DEFAULT_SERVICE_COUNT))
                .andExpect(jsonPath("$._embedded.services[?(@.name == '" + TEST_SERVICE_NAME + "')]").exists());
    }

    @Test
    public void testWpsSyncAllPublic() throws Exception {
        // Ensure default services are available
        mockMvc.perform(post("/api/contentAuthority/services/restoreDefaults").header("REMOTE_USER", fstepAdmin.getName()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/contentAuthority/services/wps/syncAllPublic").header("REMOTE_USER", fstepAdmin.getName()))
                .andExpect(status().isOk());

        verify(zooManagerClient).updateActiveZooServices(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue().size(), is(DEFAULT_SERVICE_COUNT));
    }

    @Test
    public void testWpsSyncAllPublicInDevelopment() throws Exception {
        // Ensure default services are available
        mockMvc.perform(post("/api/contentAuthority/services/restoreDefaults").header("REMOTE_USER", fstepAdmin.getName()))
                .andExpect(status().isOk());

        // Set one service to IN_DEVELOPMENT (i.e. not in the "syncAllPublic" collection)
        FstepService snapService = serviceDataService.getByName(TEST_SERVICE_NAME);
        snapService.setStatus(FstepService.Status.IN_DEVELOPMENT);
        serviceDataService.save(snapService);

        mockMvc.perform(post("/api/contentAuthority/services/wps/syncAllPublic").header("REMOTE_USER", fstepAdmin.getName()))
                .andExpect(status().isOk());

        verify(zooManagerClient).updateActiveZooServices(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue().size(), is(DEFAULT_SERVICE_COUNT - 1));
    }

    @Test
    public void testUnpublishPublish() throws Exception {
        // Ensure default services are available
        mockMvc.perform(post("/api/contentAuthority/services/restoreDefaults").header("REMOTE_USER", fstepAdmin.getName()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/contentAuthority/services/wps/syncAllPublic").header("REMOTE_USER", fstepAdmin.getName()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/contentAuthority/services/unpublish/" + serviceDataService.getByName(TEST_SERVICE_NAME).getId()).header("REMOTE_USER", fstepAdmin.getName()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/contentAuthority/services/wps/syncAllPublic").header("REMOTE_USER", fstepAdmin.getName()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/contentAuthority/services/publish/" + serviceDataService.getByName(TEST_SERVICE_NAME).getId()).header("REMOTE_USER", fstepAdmin.getName()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/contentAuthority/services/wps/syncAllPublic").header("REMOTE_USER", fstepAdmin.getName()))
                .andExpect(status().isOk());

        verify(zooManagerClient, times(3)).updateActiveZooServices(argumentCaptor.capture());

        assertThat(argumentCaptor.getAllValues().get(0).size(), is(DEFAULT_SERVICE_COUNT));
        assertThat(argumentCaptor.getAllValues().get(1).size(), is(DEFAULT_SERVICE_COUNT - 1));
        assertThat(argumentCaptor.getAllValues().get(2).size(), is(DEFAULT_SERVICE_COUNT));
    }
    
    @Test
    public void testSaveServiceCostingExpression() throws Exception {
        // Ensure default services are available
        mockMvc.perform(post("/api/contentAuthority/services/restoreDefaults").header("REMOTE_USER", fstepAdmin.getName()))
                .andExpect(status().isOk());
        CostingExpression costingExpression = new CostingExpression();
        costingExpression.setCostExpression(TEST_SERVICE_COSTING_EXPRESSION);
        costingExpression.setEstimatedCostExpression(TEST_SERVICE_ESTIMATED_COSTING_EXPRESSION);
        mockMvc.perform(post("/api/contentAuthority/services/costingExpression/" + serviceDataService.getByName(TEST_SERVICE_NAME).getId())
        		.contentType(MediaType.APPLICATION_JSON)
        		.header("REMOTE_USER", fstepAdmin.getName())
            	.content(new ObjectMapper().writeValueAsString(costingExpression)))
                .andExpect(status().isOk());
        
        verify(costingExpressionDataService).save(costingExpressionCaptor.capture());
        
        CostingExpression savedServiceCostingExpression = costingExpressionCaptor.getValue();
        assertThat(savedServiceCostingExpression.getType(), is(CostingExpression.Type.SERVICE));
        assertThat(savedServiceCostingExpression.getAssociatedId(), is(serviceDataService.getByName(TEST_SERVICE_NAME).getId()));
        assertThat(savedServiceCostingExpression.getCostExpression(), is(TEST_SERVICE_COSTING_EXPRESSION));
        assertThat(savedServiceCostingExpression.getEstimatedCostExpression(), is(TEST_SERVICE_ESTIMATED_COSTING_EXPRESSION));
    }
    
    @Test
    public void testCostingExpressionForbiddenForNormalUser() throws Exception {
        // Ensure default services are available
        mockMvc.perform(post("/api/contentAuthority/services/restoreDefaults").header("REMOTE_USER", fstepAdmin.getName()))
                .andExpect(status().isOk());
        CostingExpression costingExpression = new CostingExpression();
        costingExpression.setCostExpression(TEST_SERVICE_COSTING_EXPRESSION);
        costingExpression.setEstimatedCostExpression(TEST_SERVICE_ESTIMATED_COSTING_EXPRESSION);
        mockMvc.perform(post("/api/contentAuthority/services/costingExpression/" + serviceDataService.getByName(TEST_SERVICE_NAME).getId())
        		.contentType(MediaType.APPLICATION_JSON)
        		.header("REMOTE_USER", fstepUser.getName())
            	.content(new ObjectMapper().writeValueAsString(costingExpression)))
                .andExpect(status().isForbidden());
    }
    
    @Test
    public void testSaveCollectionCostingExpression() throws Exception {
        CostingExpression collectionCostingExpression = new CostingExpression();
        collectionCostingExpression.setCostExpression(TEST_COLLECTION_COSTING_EXPRESSION);
        mockMvc.perform(post("/api/contentAuthority/collections/costingExpression/" + collectionDataService.getByNameAndOwner(TEST_COLLECTION_NAME, fstepUser).getId())
        		.contentType(MediaType.APPLICATION_JSON)
        		.header("REMOTE_USER", fstepAdmin.getName())
            	.content(new ObjectMapper().writeValueAsString(collectionCostingExpression)))
                .andExpect(status().isOk());
        verify(costingExpressionDataService).save(costingExpressionCaptor.capture());
        
        CostingExpression savedCollectionCostingExpression = costingExpressionCaptor.getValue();
        assertThat(savedCollectionCostingExpression.getCostExpression(), is(TEST_COLLECTION_COSTING_EXPRESSION));
        assertThat(savedCollectionCostingExpression.getType(), is(CostingExpression.Type.COLLECTION));
        assertThat(savedCollectionCostingExpression.getAssociatedId(), is(collectionDataService.getByNameAndOwner(TEST_COLLECTION_NAME, fstepUser).getId()));
        assertThat(savedCollectionCostingExpression.getCostExpression(), is(TEST_COLLECTION_COSTING_EXPRESSION));
        assertThat(savedCollectionCostingExpression.getEstimatedCostExpression(), is(TEST_COLLECTION_COSTING_EXPRESSION));
    }
    
    @Test
    public void testDeleteCollectionCostingExpression() throws Exception {
        CostingExpression collectionCostingExpression = new CostingExpression();
        collectionCostingExpression.setCostExpression(TEST_COLLECTION_COSTING_EXPRESSION);
        mockMvc.perform(post("/api/contentAuthority/collections/costingExpression/" + collectionDataService.getByNameAndOwner(TEST_COLLECTION_NAME, fstepUser).getId())
        		.contentType(MediaType.APPLICATION_JSON)
        		.header("REMOTE_USER", fstepAdmin.getName())
            	.content(new ObjectMapper().writeValueAsString(collectionCostingExpression)))
                .andExpect(status().isOk());
        verify(costingExpressionDataService).save(costingExpressionCaptor.capture());
        CostingExpression savedCollectionCostingExpression = costingExpressionCaptor.getValue();
        Mockito.when(costingExpressionDataService.getCollectionCostingExpression(any(Collection.class))).thenReturn(savedCollectionCostingExpression);
        mockMvc.perform(delete("/api/contentAuthority/collections/costingExpression/" + collectionDataService.getByNameAndOwner(TEST_COLLECTION_NAME, fstepUser).getId())
        		.contentType(MediaType.APPLICATION_JSON)
        		.header("REMOTE_USER", fstepAdmin.getName()))
            	.andExpect(status().isOk());
        verify(costingExpressionDataService).delete(costingExpressionCaptor.capture());
        
    }

}