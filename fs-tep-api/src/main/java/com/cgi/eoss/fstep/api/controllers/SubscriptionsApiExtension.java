package com.cgi.eoss.fstep.api.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cgi.eoss.fstep.model.Subscription;
import com.cgi.eoss.fstep.persistence.service.SubscriptionDataService;
import com.cgi.eoss.fstep.subscriptions.SubscriptionService;

import lombok.extern.log4j.Log4j2;

@RestController
@BasePathAwareController
@RequestMapping("/subscriptions")
@Transactional
public class SubscriptionsApiExtension {
    
    private SubscriptionService subscriptionService;
    
    @Autowired
    public SubscriptionsApiExtension(SubscriptionService subscriptionService, SubscriptionDataService subscriptionDataService) {
       this.subscriptionService = subscriptionService;
    }

   
    @PostMapping("/{subscriptionId}/cancel")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#subscription, 'write')")
    public ResponseEntity<Resource<Subscription>> cancelSubscription(@ModelAttribute("subscriptionId") Subscription subscription){
    	subscriptionService.deactivateSubscription(subscription);
    	return ResponseEntity.noContent().build();
    }
}

