package com.cgi.eoss.fstep.api.controllers;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cgi.eoss.fstep.model.DefaultServiceTemplate;
import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.FstepServiceContextFile;
import com.cgi.eoss.fstep.model.FstepServiceDescriptor;
import com.cgi.eoss.fstep.model.FstepServiceTemplate;
import com.cgi.eoss.fstep.model.FstepServiceTemplateFile;
import com.cgi.eoss.fstep.persistence.service.DefaultServiceTemplateDataService;
import com.cgi.eoss.fstep.persistence.service.ServiceDataService;
import com.cgi.eoss.fstep.persistence.service.ServiceFileDataService;
import com.cgi.eoss.fstep.security.FstepSecurityService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@BasePathAwareController
@RequestMapping("/serviceTemplates")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Log4j2
public class ServiceTemplatesApiExtension {

    private final ServiceDataService serviceDataService;
    private final ServiceFileDataService serviceFileDataService;
    private final DefaultServiceTemplateDataService defaultServiceTemplateDataService;
    private final FstepSecurityService fstepSecurityService;
    
    /**
     * <p>Creates a new service from a template
     */
    
    @PostMapping("/{serviceTemplateId}/newService")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or (hasRole('EXPERT_USER') and hasPermission(#serviceTemplate, 'read'))")
    public ResponseEntity<Resource<FstepService>> createNewService(@ModelAttribute("serviceTemplateId") FstepServiceTemplate serviceTemplate, @RequestBody FstepService service) {
    	if (service.getId() != null) {
    		return new ResponseEntity<Resource<FstepService>>(HttpStatus.FORBIDDEN);
    	}
        FstepServiceDescriptor templateDescriptor = serviceTemplate.getServiceDescriptor();
        FstepServiceDescriptor serviceDescriptor = service.getServiceDescriptor();
    	if (serviceDescriptor == null) {
    		serviceDescriptor = new FstepServiceDescriptor();
    		serviceDescriptor.setId(service.getName());
    		service.setServiceDescriptor(serviceDescriptor);
    	
    	}
    	
    	if (templateDescriptor.getDataInputs() != null) {
    		serviceDescriptor.setDataInputs(templateDescriptor.getDataInputs());
    	}
    	
    	if (templateDescriptor.getDataOutputs() != null) {
    		serviceDescriptor.setDataOutputs(templateDescriptor.getDataOutputs());
    	}
        
    	service.setType(serviceTemplate.getType());
    	fstepSecurityService.updateOwnerWithCurrentUser(service);
    	serviceDataService.save(service);
    	createContextFileFromTemplateFiles(service, serviceTemplate.getTemplateFiles());
    	return new ResponseEntity<Resource<FstepService>>(new Resource<FstepService>(service), HttpStatus.CREATED);
    	
    }
    
    


	private Set<FstepServiceContextFile> createContextFileFromTemplateFiles(FstepService service,
			Set<FstepServiceTemplateFile> templateFiles) {
		return templateFiles.stream().map(templateFile -> serviceFileDataService.save(new FstepServiceContextFile(service, templateFile.getFilename(), templateFile.isExecutable(), templateFile.getContent()))).collect(Collectors.toSet());
	}


}