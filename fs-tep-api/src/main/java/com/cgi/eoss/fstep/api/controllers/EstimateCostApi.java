package com.cgi.eoss.fstep.api.controllers;

import com.cgi.eoss.fstep.security.FstepSecurityService;
import com.cgi.eoss.fstep.costing.CostingService;
import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.JobConfig;
import lombok.Builder;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>Functionality for users to retrieve cost estimations for FS-TEP activities.</p>
 */
@RestController
@BasePathAwareController
@RequestMapping("/estimateCost")
@Log4j2
public class EstimateCostApi {

    private final CostingService costingService;
    private final FstepSecurityService fstepSecurityService;

    @Autowired
    public EstimateCostApi(CostingService costingService, FstepSecurityService fstepSecurityService) {
        this.costingService = costingService;
        this.fstepSecurityService = fstepSecurityService;
    }

    @GetMapping("/jobConfig/{jobConfigId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#jobConfig, 'read')")
    public ResponseEntity estimateJobConfigCost(@ModelAttribute("jobConfigId") JobConfig jobConfig) {
        int walletBalance = fstepSecurityService.getCurrentUser().getWallet().getBalance();
        int cost = costingService.estimateJobCost(jobConfig);

        return ResponseEntity
                .status(cost > walletBalance ? HttpStatus.PAYMENT_REQUIRED : HttpStatus.OK)
                .body(CostEstimationResponse.builder().estimatedCost(cost).currentWalletBalance(walletBalance).build());
    }

    @GetMapping("/download/{fstepFileId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#fstepFile, 'read')")
    public ResponseEntity estimateDownloadCost(@ModelAttribute("fstepFileId") FstepFile fstepFile) {
        int walletBalance = fstepSecurityService.getCurrentUser().getWallet().getBalance();
        int cost = costingService.estimateDownloadCost(fstepFile);

        return ResponseEntity
                .status(cost > walletBalance ? HttpStatus.PAYMENT_REQUIRED : HttpStatus.OK)
                .body(CostEstimationResponse.builder().estimatedCost(cost).currentWalletBalance(walletBalance).build());
    }

    @Data
    @Builder
    private static class CostEstimationResponse {
        int estimatedCost;
        int currentWalletBalance;
    }

}
