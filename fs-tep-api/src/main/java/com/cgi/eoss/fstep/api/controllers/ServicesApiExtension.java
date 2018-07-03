package com.cgi.eoss.fstep.api.controllers;

import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.FstepServiceContextFile;
import com.cgi.eoss.fstep.model.FstepServiceDescriptor;
import com.cgi.eoss.fstep.model.FstepServiceDockerBuildInfo;
import com.cgi.eoss.fstep.model.FstepServiceTemplate;
import com.cgi.eoss.fstep.model.FstepServiceTemplateFile;
import com.cgi.eoss.fstep.model.FstepServiceDockerBuildInfo.Status;
import com.cgi.eoss.fstep.persistence.service.ServiceDataService;
import com.cgi.eoss.fstep.persistence.service.ServiceTemplateDataService;
import com.cgi.eoss.fstep.persistence.service.ServiceTemplateFileDataService;
import com.cgi.eoss.fstep.rpc.BuildServiceParams;
import com.cgi.eoss.fstep.rpc.BuildServiceResponse;
import com.cgi.eoss.fstep.rpc.LocalServiceLauncher;
import com.cgi.eoss.fstep.security.FstepSecurityService;
import com.cgi.eoss.fstep.services.DefaultFstepServices;
import io.grpc.stub.StreamObserver;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.stream.Collectors;

@RestController
@BasePathAwareController
@RequestMapping("/services")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Log4j2
public class ServicesApiExtension {

    @Data
    public class BuildStatus {
        private final Boolean needsBuild;
        private final FstepServiceDockerBuildInfo.Status status;
    }


    private final ServiceDataService serviceDataService;
    private final FstepSecurityService fstepSecurityService;
    private final ServiceTemplateDataService serviceTemplateDataService;
    private final ServiceTemplateFileDataService serviceTemplateFileDataService;
    private final LocalServiceLauncher localServiceLauncher;
    
    @GetMapping("/defaults")
    public Resources<FstepService> getDefaultServices() {
        // Use the default service list, but retrieve updated objects from the database
        return new Resources<>(DefaultFstepServices.getDefaultServices().stream()
                .map(s -> serviceDataService.getByName(s.getName()))
                .collect(Collectors.toList()));
    }
    
    /**
     * <p>Provides information on the status of the service Docker build</p>
     */
    @GetMapping("/{serviceId}/buildStatus")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#service, 'administration')")
    public ResponseEntity<BuildStatus> buildStatus(@ModelAttribute("serviceId") FstepService service) {
        String currentServiceFingerprint = serviceDataService.computeServiceFingerprint(service);
        boolean needsBuild = needsBuild(service, currentServiceFingerprint);
        FstepServiceDockerBuildInfo.Status status;
        if (service.getDockerBuildInfo() == null) {
            status = FstepServiceDockerBuildInfo.Status.NOT_STARTED;
        }
        else {
            status = service.getDockerBuildInfo().getDockerBuildStatus();
        }
        BuildStatus buildStatus = new BuildStatus(needsBuild, status);
        return new ResponseEntity<BuildStatus>(buildStatus, HttpStatus.OK);
    }
    
    private boolean needsBuild(FstepService fstepService, String currentServiceFingerprint) {
        if (fstepService.getDockerBuildInfo() == null) {
            return true;
        }
        if (fstepService.getDockerBuildInfo().getDockerBuildStatus() == FstepServiceDockerBuildInfo.Status.ONGOING) {
            return false;
        }
        if (fstepService.getDockerBuildInfo().getLastBuiltFingerprint() == null) {
            return true;
        }
        return !currentServiceFingerprint.equals(fstepService.getDockerBuildInfo().getLastBuiltFingerprint());
    }
    
    /**
     * <p>Creates a new service from a template
     */
    
    @PostMapping("/{serviceId}/newTemplate")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or (hasRole('EXPERT_USER') and hasPermission(#service, 'administration'))")
    public ResponseEntity<Resource<FstepServiceTemplate>> createNewServiceTemplate(@ModelAttribute("serviceId") FstepService service, @RequestBody FstepServiceTemplate serviceTemplate) {
    	if (serviceTemplate.getId() != null) {
    		return new ResponseEntity<Resource<FstepServiceTemplate>>(HttpStatus.FORBIDDEN);
    	}
        FstepServiceDescriptor serviceDescriptor = service.getServiceDescriptor();
        FstepServiceDescriptor templateDescriptor = serviceTemplate.getServiceDescriptor();
        
        if (templateDescriptor == null) {
        	templateDescriptor = new FstepServiceDescriptor();
    		templateDescriptor.setId(serviceTemplate.getName());
    		templateDescriptor.setDescription(serviceTemplate.getDescription());
    		serviceTemplate.setServiceDescriptor(templateDescriptor);
    	}
    	populateTemplateDescriptorFromServiceDescriptor(serviceDescriptor, templateDescriptor);
        
    	serviceTemplate.setType(service.getType());
    	serviceTemplate.setRequiredResources(service.getRequiredResources());
    	fstepSecurityService.updateOwnerWithCurrentUser(serviceTemplate);
    	serviceTemplateDataService.save(serviceTemplate);
    	createTemplateFilesFromContextFile(serviceTemplate, service.getContextFiles());
    	return new ResponseEntity<Resource<FstepServiceTemplate>>(new Resource<FstepServiceTemplate>(serviceTemplate), HttpStatus.CREATED);
    	
    }

	private void populateTemplateDescriptorFromServiceDescriptor(FstepServiceDescriptor serviceDescriptor,
			FstepServiceDescriptor templateDescriptor) {
		if (serviceDescriptor.getDataInputs() != null) {
			templateDescriptor.setDataInputs(serviceDescriptor.getDataInputs());
		}
		if (serviceDescriptor.getDataOutputs() != null) {
			templateDescriptor.setDataOutputs(serviceDescriptor.getDataOutputs());
		}
        templateDescriptor.setStatusSupported(serviceDescriptor.isStatusSupported());
        templateDescriptor.setStoreSupported(serviceDescriptor.isStoreSupported());
        templateDescriptor.setServiceProvider(serviceDescriptor.getServiceProvider());
        templateDescriptor.setServiceType(serviceDescriptor.getServiceType());
	}
    
    private Set<FstepServiceTemplateFile> createTemplateFilesFromContextFile(FstepServiceTemplate serviceTemplate,
			Set<FstepServiceContextFile> contextFiles) {
		return contextFiles.stream().map(contextFile -> serviceTemplateFileDataService.save(new FstepServiceTemplateFile(serviceTemplate, contextFile.getFilename(), contextFile.isExecutable(), contextFile.getContent()))).collect(Collectors.toSet());
	}
    
    /**
     * <p>Builds the service docker image
     * <p>Build is launched asynchronously</p>
     */
    @PostMapping("/{serviceId}/build")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#service, 'administration')")
    public ResponseEntity build(@ModelAttribute("serviceId") FstepService service) {
        FstepServiceDockerBuildInfo dockerBuildInfo = service.getDockerBuildInfo();
        
        if (dockerBuildInfo != null && dockerBuildInfo.getDockerBuildStatus().equals(FstepServiceDockerBuildInfo.Status.ONGOING)) {
            return new ResponseEntity<>("A build is already ongoing",  HttpStatus.CONFLICT);
        }
        else {
            String currentServiceFingerprint = serviceDataService.computeServiceFingerprint(service);
            if (needsBuild(service, currentServiceFingerprint)) {
                LOG.info("Building service via REST API: {}", service.getName());
                if (dockerBuildInfo == null) {
                    dockerBuildInfo = new FstepServiceDockerBuildInfo();
                    service.setDockerBuildInfo(dockerBuildInfo);
                }
                dockerBuildInfo.setDockerBuildStatus(Status.ONGOING);
                serviceDataService.save(service);
                BuildServiceParams.Builder buildServiceParamsBuilder = BuildServiceParams.newBuilder()
                        .setUserId(fstepSecurityService.getCurrentUser().getName())
                        .setServiceId(String.valueOf(service.getId()))
                        .setBuildFingerprint(currentServiceFingerprint);
                BuildServiceParams buildServiceParams = buildServiceParamsBuilder.build();
                buildService(service, buildServiceParams);
                return new ResponseEntity<>( HttpStatus.ACCEPTED);
            }
            else {
                return new ResponseEntity<>( HttpStatus.OK);
            }
        }
    }

    private void buildService(FstepService fstepService, BuildServiceParams buildServiceParams) {
        serviceDataService.save(fstepService);
        BuildServiceObserver responseObserver = new BuildServiceObserver();
        localServiceLauncher.asyncBuildService(buildServiceParams, responseObserver);
    }
    

    public class BuildServiceObserver implements StreamObserver<BuildServiceResponse> {
        
        public BuildServiceObserver() {
        }

        @Override
        public void onNext(BuildServiceResponse value) {
        }

        @Override
        public void onError(Throwable t) {
           
            
        }

        @Override
        public void onCompleted() {
            
        }

    }


}