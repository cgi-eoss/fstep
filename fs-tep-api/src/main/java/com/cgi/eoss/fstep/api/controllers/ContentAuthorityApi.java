package com.cgi.eoss.fstep.api.controllers;

import com.cgi.eoss.fstep.security.FstepSecurityService;
import com.cgi.eoss.fstep.model.Collection;
import com.cgi.eoss.fstep.model.CostingExpression;
import com.cgi.eoss.fstep.model.CostingExpression.Type;
import com.cgi.eoss.fstep.model.DefaultServiceTemplate;
import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.FstepServiceTemplate;
import com.cgi.eoss.fstep.model.PublishingRequest;
import com.cgi.eoss.fstep.orchestrator.zoo.ZooManagerClient;
import com.cgi.eoss.fstep.persistence.service.CostingExpressionDataService;
import com.cgi.eoss.fstep.persistence.service.DefaultServiceTemplateDataService;
import com.cgi.eoss.fstep.persistence.service.PublishingRequestDataService;
import com.cgi.eoss.fstep.persistence.service.ServiceDataService;
import com.cgi.eoss.fstep.services.DefaultFstepServices;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>Functionality for users with the CONTENT_AUTHORITY Role.</p>
 */
@RestController
@BasePathAwareController
@RequestMapping("/contentAuthority")
@Log4j2
public class ContentAuthorityApi {

    private final FstepSecurityService fstepSecurityService;
    private final ZooManagerClient zooManagerClient;
    private final PublishingRequestDataService publishingRequestsDataService;
    private final CostingExpressionDataService costingExpressionDataService;
    private final ServiceDataService serviceDataService;
    private final DefaultServiceTemplateDataService defaultServiceTemplateDataService;

    @Autowired
    public ContentAuthorityApi(FstepSecurityService fstepSecurityService, ZooManagerClient zooManagerClient, PublishingRequestDataService publishingRequestsDataService, ServiceDataService serviceDataService, CostingExpressionDataService costingExpressionDataService, DefaultServiceTemplateDataService defaultServiceTemplateDataService) {
        this.fstepSecurityService = fstepSecurityService;
        this.zooManagerClient = zooManagerClient;
        this.publishingRequestsDataService = publishingRequestsDataService;
        this.serviceDataService = serviceDataService;
        this.costingExpressionDataService = costingExpressionDataService;
        this.defaultServiceTemplateDataService = defaultServiceTemplateDataService;
    }

    @PostMapping("/services/restoreDefaults")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    public void restoreDefaultServices() {
        Set<FstepService> defaultServices = DefaultFstepServices.getDefaultServices();

        for (FstepService service : defaultServices) {
            LOG.info("Restoring default service: {}", service.getName());

            // If the service already exists, synchronise the IDs (and associated file IDs) to avoid constraint errors
            Optional.ofNullable(serviceDataService.getByName(service.getName())).ifPresent((FstepService existing) -> {
                service.setId(existing.getId());
                service.getContextFiles().forEach(newFile -> {
                    existing.getContextFiles().stream()
                            .filter(existingFile -> existingFile.getFilename().equals(newFile.getFilename()))
                            .findFirst()
                            .ifPresent(existingFile -> newFile.setId(existingFile.getId()));
                });
            });

            service.setOwner(fstepSecurityService.refreshPersistentUser(service.getOwner()));
            serviceDataService.save(service);
            publishService(service);
        }
    }

    @PostMapping("/services/wps/syncAllPublic")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    public void wpsSyncAllPublic() {
        // Find all Status.AVAILABLE, then filter for those visible to PUBLIC
        List<FstepService> publicServices = serviceDataService.findAllAvailable().stream()
                .filter(s -> fstepSecurityService.isPublic(FstepService.class, s.getId()))
                .collect(Collectors.toList());
        zooManagerClient.updateActiveZooServices(publicServices);
    }

    @PostMapping("/services/publish/{serviceId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    public void publishService(@ModelAttribute("serviceId") FstepService service) {
        service.setStatus(FstepService.Status.AVAILABLE);
        serviceDataService.save(service);

        fstepSecurityService.publish(FstepService.class, service.getId());
        publishingRequestsDataService.findRequestsForPublishing(service).forEach(request -> {
            request.setStatus(PublishingRequest.Status.GRANTED);
            publishingRequestsDataService.save(request);
        });
    }
    
    @PostMapping("/services/unpublish/{serviceId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    public void unpublishService(@ModelAttribute("serviceId") FstepService service) {
        fstepSecurityService.unpublish(FstepService.class, service.getId());
    }
    
    @PostMapping("/services/costingExpression/{serviceId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    public void setServiceCostingExpression(@ModelAttribute("serviceId") FstepService service, @RequestBody CostingExpression inputCostingExpression) {
    	CostingExpression costingExpression = new CostingExpression(Type.SERVICE, service.getId(), 
    			inputCostingExpression.getCostExpression(), inputCostingExpression.getEstimatedCostExpression() != null? inputCostingExpression.getEstimatedCostExpression(): inputCostingExpression.getCostExpression());
    	costingExpressionDataService.save(costingExpression);
    }
    
    @DeleteMapping("/services/costingExpression/{serviceId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    public void deleteServiceCostingExpression(@ModelAttribute("serviceId") FstepService service) {
    	CostingExpression costingExpression = costingExpressionDataService.getServiceCostingExpression(service);
    	if (costingExpression != null) {
    		costingExpressionDataService.delete(costingExpression);
    	}
    }
    
    
    @PostMapping("/collections/costingExpression/{collectionId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    public void setCollectionCostingExpression(@ModelAttribute("collectionId") Collection collection, @RequestBody CostingExpression inputCostingExpression) {
    	CostingExpression costingExpression = new CostingExpression(Type.COLLECTION, collection.getId(), inputCostingExpression.getCostExpression(), inputCostingExpression.getEstimatedCostExpression() != null? inputCostingExpression.getEstimatedCostExpression(): inputCostingExpression.getCostExpression());
    	costingExpressionDataService.save(costingExpression);
    }
    
    @DeleteMapping("/collections/costingExpression/{collectionId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    public void deleteCollectionCostingExpression(@ModelAttribute("collectionId") Collection collection) {
    	CostingExpression costingExpression = costingExpressionDataService.getCollectionCostingExpression(collection);
    	if (costingExpression != null) {
    		costingExpressionDataService.delete(costingExpression);
    	}
    }
    
    @PostMapping("/serviceTemplates/publish/{serviceTemplateId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    public void publishServiceTemplate(@ModelAttribute("serviceTemplateId") FstepServiceTemplate serviceTemplate) {
        fstepSecurityService.publish(FstepServiceTemplate.class, serviceTemplate.getId());
        publishingRequestsDataService.findRequestsForPublishingServiceTemplate(serviceTemplate).forEach(request -> {
            request.setStatus(PublishingRequest.Status.GRANTED);
            publishingRequestsDataService.save(request);
        });
    }
    
    @PostMapping("/serviceTemplates/unpublish/{serviceTemplateId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    public void unpublishServiceTemplate(@ModelAttribute("serviceTemplateId") FstepServiceTemplate serviceTemplate) {
        fstepSecurityService.unpublish(FstepServiceTemplate.class, serviceTemplate.getId());
    }
    
    /**
     * <p>Makes the template default for its type
     */
    
    @PostMapping("/serviceTemplates/makeDefault/{serviceTemplateId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    public ResponseEntity<Void> makeTemplateDefault(@ModelAttribute("serviceTemplateId") FstepServiceTemplate serviceTemplate) {
    	DefaultServiceTemplate defaultServiceTemplate = defaultServiceTemplateDataService.getByServiceType(serviceTemplate.getType());
    	if (defaultServiceTemplate != null) {
    		if (!defaultServiceTemplate.getServiceTemplate().getId().equals(serviceTemplate.getId())) {
    			defaultServiceTemplate.setServiceTemplate(serviceTemplate);
    			defaultServiceTemplateDataService.save(defaultServiceTemplate);
        	}
    		return new ResponseEntity<>(HttpStatus.OK);
    	}
		defaultServiceTemplate = new DefaultServiceTemplate();
		defaultServiceTemplate.setServiceType(serviceTemplate.getType());
		defaultServiceTemplate.setServiceTemplate(serviceTemplate);
		defaultServiceTemplateDataService.save(defaultServiceTemplate);
    	return new ResponseEntity<>(HttpStatus.OK);
    }
    
    @PostMapping("/collections/publish/{collectionId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    public void publishServiceTemplate(@ModelAttribute("collectionId") Collection collection) {
        fstepSecurityService.publish(Collection.class, collection.getId());
        publishingRequestsDataService.findRequestsForPublishingCollection(collection).forEach(request -> {
            request.setStatus(PublishingRequest.Status.GRANTED);
            publishingRequestsDataService.save(request);
        });
    }
    
    @PostMapping("/collections/unpublish/{collectionId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    public void unpublishCollection(@ModelAttribute("collectionId") Collection collection) {
        fstepSecurityService.unpublish(Collection.class, collection.getId());
    }

}
