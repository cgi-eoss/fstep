package com.cgi.eoss.fstep.api.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cgi.eoss.fstep.costing.CostingService;
import com.cgi.eoss.fstep.model.Collection;
import com.cgi.eoss.fstep.model.CostingExpression;
import com.cgi.eoss.fstep.model.FstepService;

/**
 * <p>Functionality for users to retrieve cost of FS-TEP resources.</p>
 */
@RestController
@BasePathAwareController
@RequestMapping("/cost")
public class CostApi {

    private final CostingService costingService;
    
    @Autowired
    public CostApi(CostingService costingService) {
        this.costingService = costingService;
    }

    @GetMapping("/service/{serviceId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#service, 'read')")
    public CostingExpression getServiceCost(@ModelAttribute("serviceId") FstepService service) {
        return costingService.getServiceCostingExpression(service);
    }
    
    @GetMapping("/collection/{collectionId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#collection, 'read')")
    public CostingExpression getCollectionCost(@ModelAttribute("collectionId") Collection collection) {
    	 return costingService.getCollectionCostingExpression(collection);
    }
}
