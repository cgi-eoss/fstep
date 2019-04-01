package com.cgi.eoss.fstep.orchestrator.service;

import static java.util.stream.Collectors.toSet;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cgi.eoss.fstep.catalogue.CatalogueService;
import com.cgi.eoss.fstep.model.CostQuotation;
import com.cgi.eoss.fstep.costing.CostingService;
import com.cgi.eoss.fstep.model.Job;
import com.cgi.eoss.fstep.model.JobConfig;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.rpc.GrpcUtil;
import com.cgi.eoss.fstep.rpc.JobParam;
import com.google.common.collect.Multimap;

@Component
public class JobValidator {

	private PlatformParameterExtractor platformParameterExtractor;
	
	private CostingService costingService;
	private CatalogueService catalogueService;
	
	@Autowired
	public JobValidator(CostingService costingService, CatalogueService catalogueService) {
		this.costingService = costingService;
		this.catalogueService = catalogueService;
		platformParameterExtractor = new PlatformParameterExtractor();
	}
	
	public void checkCost(User user, JobConfig jobConfig) {
        CostQuotation costEstimate = costingService.estimateJobCost(jobConfig);
        Integer cost = costEstimate.getCost();
        if (cost > user.getWallet().getBalance()) {
            throw new ServiceExecutionException(
                    "Estimated cost (" + cost + " coins) exceeds current wallet balance");
        }
    }
	
	 public void checkCost(User user, JobConfig config, List<String> newInputs) {
	    	CostQuotation singleJobCostEstimate = costingService.estimateSingleRunJobCost(config);
	        int estimatedCost = newInputs.size() * singleJobCostEstimate.getCost();
	        if (estimatedCost > user.getWallet().getBalance()) {
	            throw new ServiceExecutionException(
	                    "Estimated cost (" + estimatedCost + " coins) exceeds current wallet balance");
	        }
	    }
	
	public void checkCost(User user, Job job) {
		Integer cost = getJobCost(job);
        if (cost > user.getWallet().getBalance()) {
            throw new ServiceExecutionException(
                    "Estimated cost (" + cost + " coins) exceeds current wallet balance");
        }
    }
	
	public void checkCost(User user, Collection<Job> jobs) {
		int totalCost = 0;
		for (Job job: jobs) {
			totalCost += getJobCost(job);
		}
		if (totalCost > user.getWallet().getBalance()) {
            throw new ServiceExecutionException(
                    "Estimated cost (" + totalCost + " coins) exceeds current wallet balance");
        }
    }
	
	private Integer getJobCost(Job job) {
		CostQuotation jobCost = job.getCostQuotation();
		if (jobCost == null) {
			jobCost = costingService.estimateJobCost(job.getConfig());
		}
        Integer cost = jobCost.getCost();
		return cost;
	}
	
    
    public boolean checkInputs(User user, List<JobParam> inputsList) {
        Multimap<String, String> inputs = GrpcUtil.paramsListToMap(inputsList);

        Set<URI> inputUris = inputs.entries().stream().filter(e -> this.isValidUri(e.getValue()))
                .flatMap(e -> Arrays.stream(StringUtils.split(e.getValue(), ',')).map(URI::create))
                .collect(toSet());

        return inputUris.stream().allMatch(uri -> catalogueService.canUserRead(user, uri));
    }

    public boolean checkInputList(User user, List<String> inputsList) {
        return inputsList.stream().filter(e -> this.isValidUri(e)).map(URI::create).collect(toSet())
                .stream().allMatch(uri -> catalogueService.canUserRead(user, uri));
    }
        
    public boolean checkAccessToOutputCollection(Job job) {
    	User user = job.getOwner();
    	try {
        	return platformParameterExtractor.getCollectionSpecs(job)
        			.values().stream()
                    .allMatch(collectionId -> catalogueService.canUserWrite(user, collectionId));
        } catch (IOException e) {
            return false;
        }
    }
    
    private boolean isValidUri(String test) {
        try {
            return URI.create(test).getScheme() != null;
        } catch (Exception unused) {
            return false;
        }
    }
}
