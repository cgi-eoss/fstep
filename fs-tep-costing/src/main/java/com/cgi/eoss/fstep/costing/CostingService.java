package com.cgi.eoss.fstep.costing;

import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.Job;
import com.cgi.eoss.fstep.model.JobConfig;
import com.cgi.eoss.fstep.model.Wallet;

/**
 * <p>Service to expose FS-TEP activity cost estimations and the charge mechanism.</p>
 */
public interface CostingService {

    Integer estimateJobCost(JobConfig jobConfig);
    
    Integer estimateSingleRunJobCost(JobConfig jobConfig);
    
    Integer estimateDownloadCost(FstepFile download);

    void chargeForJob(Wallet wallet, Job job);

    void chargeForDownload(Wallet wallet, FstepFile download);

    void refundUser(Wallet wallet, Job job);

}
