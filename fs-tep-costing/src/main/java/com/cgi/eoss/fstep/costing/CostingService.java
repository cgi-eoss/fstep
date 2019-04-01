package com.cgi.eoss.fstep.costing;

import com.cgi.eoss.fstep.model.CostQuotation;
import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.Job;
import com.cgi.eoss.fstep.model.JobConfig;
import com.cgi.eoss.fstep.model.JobProcessing;
import com.cgi.eoss.fstep.model.Wallet;

/**
 * <p>Service to expose FS-TEP activity cost estimations and the charge mechanism.</p>
 */
public interface CostingService {

    CostQuotation estimateJobCost(JobConfig jobConfig);
    
    CostQuotation estimateJobRelaunchCost(Job job);
    
    CostQuotation estimateSingleRunJobCost(JobConfig jobConfig);
    
    CostQuotation estimateDownloadCost(FstepFile download);

    void chargeForDownload(Wallet wallet, FstepFile download);
    
	void chargeForJobProcessing(Wallet wallet, JobProcessing jobProcessing);
	
	void transactForJobProcessing(Wallet wallet, JobProcessing jobProcessing, int amount);
	
	void refundJobProcessing(Wallet wallet, JobProcessing jobProcessing);

}
