package com.cgi.eoss.fstep.api.controllers;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cgi.eoss.fstep.model.UsageType;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.quotas.UsageService;
import com.cgi.eoss.fstep.security.FstepSecurityService;

/**
 * <p>Functionality for platform usage metrics retrieval </p>
 */
@RestController
@BasePathAwareController
@RequestMapping("/usage")
public class UsageApi {

	private final FstepSecurityService fstepSecurityService;
    private final UsageService usageService;
    @Autowired
    public UsageApi(FstepSecurityService fstepSecurityService, UsageService usageService) {
    	this.fstepSecurityService = fstepSecurityService;
    	this.usageService = usageService;
    }
    
    /**
     * Get current usage of storage for FstepFiles
     * @return File size in MB
     */
    @GetMapping("/files/storage")
    public ResponseEntity<Long> filesStorage() {
    	User user = fstepSecurityService.getCurrentUser();
    	Optional<Long> usage = usageService.getUsage(user, UsageType.FILES_STORAGE_MB);
    	if (!usage.isPresent())
    		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    	return new ResponseEntity<>(usage.get(), HttpStatus.OK);
    }
    
    /**
     * Get current usage of storage for the persistent folder
     * @return File size in MB
     * @throws InterruptedException 
     */
    @GetMapping("/persistentFolder/storage")
    public ResponseEntity<Long> persistentFolderStorage() {
    	User user = fstepSecurityService.getCurrentUser();
    	Optional<Long> usage = usageService.getUsage(user, UsageType.PERSISTENT_STORAGE_MB);
    	if (!usage.isPresent())
    		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    	return new ResponseEntity<>(usage.get(), HttpStatus.OK);
 	}
    
    @GetMapping("/value")
    public ResponseEntity<Long> getUsageByType(@RequestParam("usageType") UsageType usageType) {
    	User user = fstepSecurityService.getCurrentUser();
    	Optional<Long> usage = usageService.getUsage(user, usageType);
    	if (!usage.isPresent())
    		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    	return new ResponseEntity<>(usage.get(), HttpStatus.OK);
 	}
}
