package com.cgi.eoss.fstep.api.controllers;

import com.cgi.eoss.fstep.security.FstepSecurityService;
import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.PublishingRequest;
import com.cgi.eoss.fstep.orchestrator.zoo.ZooManagerClient;
import com.cgi.eoss.fstep.persistence.service.PublishingRequestDataService;
import com.cgi.eoss.fstep.persistence.service.ServiceDataService;
import com.cgi.eoss.fstep.services.DefaultFstepServices;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
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
    private final ServiceDataService serviceDataService;

    @Autowired
    public ContentAuthorityApi(FstepSecurityService fstepSecurityService, ZooManagerClient zooManagerClient, PublishingRequestDataService publishingRequestsDataService, ServiceDataService serviceDataService) {
        this.fstepSecurityService = fstepSecurityService;
        this.zooManagerClient = zooManagerClient;
        this.publishingRequestsDataService = publishingRequestsDataService;
        this.serviceDataService = serviceDataService;
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

}
