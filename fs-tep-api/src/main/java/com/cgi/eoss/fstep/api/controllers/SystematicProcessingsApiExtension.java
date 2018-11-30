package com.cgi.eoss.fstep.api.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cgi.eoss.fstep.model.SystematicProcessing;
import com.cgi.eoss.fstep.model.SystematicProcessing.Status;
import com.cgi.eoss.fstep.persistence.service.SystematicProcessingDataService;

import lombok.extern.log4j.Log4j2;

@RestController
@BasePathAwareController
@RequestMapping("/systematicProcessings")
@Transactional
@Log4j2
public class SystematicProcessingsApiExtension {


    
    private SystematicProcessingDataService systematicProcessingDataService;

	@Autowired
    public SystematicProcessingsApiExtension(SystematicProcessingDataService systematicProcessingDataService) {
       this.systematicProcessingDataService = systematicProcessingDataService;
    }

   
    @PostMapping("/{systematicProcessingId}/terminate")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#systematicProcessing, 'write')")
    public ResponseEntity terminate(@ModelAttribute("systematicProcessingId") SystematicProcessing systematicProcessing) {
    	systematicProcessing.setStatus(Status.COMPLETED);
    	systematicProcessingDataService.save(systematicProcessing);
    	return ResponseEntity.noContent().build();
    }

}