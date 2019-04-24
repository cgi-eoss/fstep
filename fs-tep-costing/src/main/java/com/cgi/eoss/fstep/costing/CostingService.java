package com.cgi.eoss.fstep.costing;

import com.cgi.eoss.fstep.model.Collection;
import com.cgi.eoss.fstep.model.CostQuotation;
import com.cgi.eoss.fstep.model.CostingExpression;
import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.Job;
import com.cgi.eoss.fstep.model.JobConfig;
import com.cgi.eoss.fstep.model.JobProcessing;
import com.cgi.eoss.fstep.model.Subscription;
import com.cgi.eoss.fstep.model.Wallet;

/**
 * <p>Service to expose FS-TEP activity cost estimations and the charge mechanism.</p>
 */
public interface CostingService {

    CostQuotation estimateJobCost(JobConfig jobConfig);
    
    CostQuotation estimateJobRelaunchCost(Job job);
    
    CostQuotation estimateSingleRunJobCost(JobConfig jobConfig);
    
    CostQuotation estimateDownloadCost(FstepFile download);
    
    CostQuotation getSubscriptionCost(Subscription subscription);
    
    void chargeForDownload(Wallet wallet, FstepFile download);
    
	void chargeForJobProcessing(Wallet wallet, JobProcessing jobProcessing);
	
	void transactForJobProcessing(Wallet wallet, JobProcessing jobProcessing, int amount);
	
	void refundJobProcessing(Wallet wallet, JobProcessing jobProcessing);
	
	void chargeForSubscription(Wallet wallet, Subscription subscription);

	void transactForSubscription(Wallet wallet, Subscription subscription, int amount);
	
	CostingExpression getServiceCostingExpression(FstepService service);
	
	CostingExpression getCollectionCostingExpression(Collection collection);
	
}
