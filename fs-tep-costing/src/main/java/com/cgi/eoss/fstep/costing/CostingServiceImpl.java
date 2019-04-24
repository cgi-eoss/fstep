package com.cgi.eoss.fstep.costing;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.expression.ExpressionParser;
import org.springframework.transaction.annotation.Transactional;

import com.cgi.eoss.fstep.model.Collection;
import com.cgi.eoss.fstep.model.CostQuotation;
import com.cgi.eoss.fstep.model.CostingExpression;
import com.cgi.eoss.fstep.model.Databasket;
import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.Job;
import com.cgi.eoss.fstep.model.JobConfig;
import com.cgi.eoss.fstep.model.JobProcessing;
import com.cgi.eoss.fstep.model.Subscription;
import com.cgi.eoss.fstep.model.Wallet;
import com.cgi.eoss.fstep.model.WalletTransaction;
import com.cgi.eoss.fstep.model.CostQuotation.Recurrence;
import com.cgi.eoss.fstep.persistence.service.CostingExpressionDataService;
import com.cgi.eoss.fstep.persistence.service.DatabasketDataService;
import com.cgi.eoss.fstep.persistence.service.JobDataService;
import com.cgi.eoss.fstep.persistence.service.WalletDataService;
import com.google.common.base.Strings;

/**
 * <p>Default implementation of {@link CostingService}.</p>
 */
public class CostingServiceImpl implements CostingService {

    private final ExpressionParser expressionParser;
    private final CostingExpressionDataService costingDataService;
    private final WalletDataService walletDataService;
    private final CostingExpression defaultJobCostingExpression;
    private final CostingExpression defaultDownloadCostingExpression;
	private final DatabasketDataService databasketDataService;
	private final JobDataService jobDataService;
	public CostingServiceImpl(ExpressionParser costingExpressionParser, CostingExpressionDataService costingDataService,
                              WalletDataService walletDataService, DatabasketDataService databasketDataService, JobDataService jobDataService, String defaultJobCostingExpression, String defaultDownloadCostingExpression) {
        this.expressionParser = costingExpressionParser;
        this.costingDataService = costingDataService;
        this.walletDataService = walletDataService;
        this.databasketDataService = databasketDataService;
        this.jobDataService = jobDataService;
        this.defaultJobCostingExpression = CostingExpression.builder().costExpression(defaultJobCostingExpression).build();
        this.defaultDownloadCostingExpression = CostingExpression.builder().costExpression(defaultDownloadCostingExpression).build();
    }

    @Override
    public CostQuotation estimateJobCost(JobConfig jobConfig) {
        CostQuotation singleJobCost =  estimateSingleRunJobCost(jobConfig);
        if (jobConfig.getService().getType() == FstepService.Type.PARALLEL_PROCESSOR) {
        		return new CostQuotation(calculateNumberOfInputs(jobConfig.getInputs().get("parallelInputs")) * singleJobCost.getCost(), singleJobCost.getRecurrence());
        }
        else {
        		return singleJobCost;
        }
    }
    
    @Override
    public CostQuotation estimateJobRelaunchCost(Job job) {
        if (job.isParent()) {
        	job = jobDataService.refreshFull(job.getId());
        	return new CostQuotation(job.getSubJobs()
        			.stream()
        			.filter(j -> j.getStatus() == Job.Status.ERROR && j.getStage() != null && !j.getStage().equals("Step 1 of 3: Data-Fetch"))
        			.mapToInt(j -> estimateSingleRunJobCost(j.getConfig()).getCost())
        			.sum(), Recurrence.ONE_OFF);
        }
        else {
        	if( job.getStage() == null || job.getStage().equals("Step 1 of 3: Data-Fetch")) {
        		return new CostQuotation(0, Recurrence.ONE_OFF);
        	}
        	return estimateSingleRunJobCost(job.getConfig());
        }
    }
    
    @Override
    public CostQuotation estimateSingleRunJobCost(JobConfig jobConfig) {
    	Optional<CostingExpression> serviceCostingExpression = Optional.ofNullable(costingDataService.getServiceCostingExpression(jobConfig.getService()));
    	Recurrence recurrence;
    	CostingExpression jobCostingExpression;
    	if (serviceCostingExpression.isPresent()){
    		recurrence = Recurrence.ONE_OFF;
    		jobCostingExpression = serviceCostingExpression.get();
    	} 
    	else {
    		recurrence = Recurrence.HOURLY;
    		jobCostingExpression = defaultJobCostingExpression;
    	}
    	
        String estimateExpression = Strings.isNullOrEmpty(jobCostingExpression.getEstimatedCostExpression())
                ? jobCostingExpression.getCostExpression()
                : jobCostingExpression.getEstimatedCostExpression();
        Integer cost = ((Number) expressionParser.parseExpression(estimateExpression).getValue(jobConfig)).intValue();
        return new CostQuotation(cost, recurrence);
    }

	private int calculateNumberOfInputs(java.util.Collection<String> inputUris){
		int inputCount = 1;
		for (String inputUri: inputUris) {
			if (inputUri.startsWith("fstep://databasket")) {
				Databasket dataBasket = getDatabasketFromUri(inputUri);
				inputCount = dataBasket.getFiles().size();
			}
			else {
				if(inputUri.contains((","))) {
					inputCount = inputUri.split(",").length;
				}
				else {
					inputCount = 1;
				}
			}
		}
		return inputCount;
		
	}
	
	private Databasket getDatabasketFromUri(String uri) {
        Matcher uriIdMatcher = Pattern.compile(".*/([0-9]+)$").matcher(uri);
        if (!uriIdMatcher.matches()) {
        		throw new RuntimeException("Failed to load databasket for URI: " + uri);
        }
        	Long databasketId = Long.parseLong(uriIdMatcher.group(1));
        	Databasket databasket = Optional.ofNullable(databasketDataService.getById(databasketId)).orElseThrow(() -> new RuntimeException("Failed to load databasket for ID " + databasketId));
        	return databasket;
    }
    
    @Override
    public CostQuotation estimateDownloadCost(FstepFile fstepFile) {
        CostingExpression costingExpression = getCostingExpression(fstepFile);

        String expression = Strings.isNullOrEmpty(costingExpression.getEstimatedCostExpression())
                ? costingExpression.getCostExpression()
                : costingExpression.getEstimatedCostExpression();
        Integer cost = ((Number) expressionParser.parseExpression(expression).getValue(fstepFile)).intValue();
        return new CostQuotation(cost, Recurrence.ONE_OFF);
    }
    
    @Override
    public CostQuotation getSubscriptionCost(Subscription subscription) {
    	CostQuotation costQuotation = subscription.getSubscriptionPlan().getCostQuotation();
		return new CostQuotation(subscription.getQuantity() * costQuotation.getCost(), costQuotation.getRecurrence());
    }
    
    @Override
    @Transactional
    public void chargeForJobProcessing(Wallet wallet, JobProcessing jobProcessing) {
    	CostQuotation costQuotation = jobProcessing.getJob().getCostQuotation();
    	if (costQuotation == null) {
    		costQuotation = getCostQuotation(jobProcessing.getJob());
    	}
        int cost = costQuotation.getCost();
        WalletTransaction walletTransaction = WalletTransaction.builder()
                .wallet(walletDataService.refreshFull(wallet))
                .balanceChange(-cost)
                .type(WalletTransaction.Type.JOB_PROCESSING)
                .associatedId(jobProcessing.getId())
                .transactionTime(LocalDateTime.now())
                .build();
        walletDataService.transact(walletTransaction);
    }
    
    @Override
    @Transactional
    public void transactForJobProcessing(Wallet wallet, JobProcessing jobProcessing, int amount) {
    	WalletTransaction walletTransaction = WalletTransaction.builder()
                .wallet(walletDataService.refreshFull(wallet))
                .balanceChange(amount)
                .type(WalletTransaction.Type.JOB_PROCESSING)
                .associatedId(jobProcessing.getId())
                .transactionTime(LocalDateTime.now())
                .build();
        walletDataService.transact(walletTransaction);
    }
    
    @Override
    @Transactional
    public void refundJobProcessing(Wallet wallet, JobProcessing jobProcessing) {
    	CostQuotation costQuotation = jobProcessing.getJob().getCostQuotation();
    	if (costQuotation == null) {
    		costQuotation = getCostQuotation(jobProcessing.getJob());
    	}
        WalletTransaction walletTransaction =
                WalletTransaction.builder().wallet(walletDataService.refreshFull(wallet)).balanceChange(costQuotation.getCost())
                        .type(WalletTransaction.Type.JOB_PROCESSING).associatedId(jobProcessing.getId()).transactionTime(LocalDateTime.now()).build();
        walletDataService.transact(walletTransaction);
    }
    
    @Override
    public CostingExpression getServiceCostingExpression(FstepService fstepService) {
    	Optional<CostingExpression> serviceCostingExpression = Optional.ofNullable(costingDataService.getServiceCostingExpression(fstepService));
    	if (serviceCostingExpression.isPresent()) {
    		return serviceCostingExpression.get();
    	}
    	return defaultJobCostingExpression;
    }
    
    @Override
    public CostingExpression getCollectionCostingExpression(Collection collection) {
    	Optional<CostingExpression> collectionCostingExpression = Optional.ofNullable(costingDataService.getCollectionCostingExpression(collection));
    	if (collectionCostingExpression.isPresent()) {
    		return collectionCostingExpression.get();
    	}
    	return defaultDownloadCostingExpression;
    }
    
    private CostQuotation getCostQuotation(Job job) {
    	Optional<CostingExpression> serviceCostingExpression = Optional.ofNullable(costingDataService.getServiceCostingExpression(job.getConfig().getService()));
    	Recurrence recurrence;
    	CostingExpression jobCostingExpression;
    	if (serviceCostingExpression.isPresent()){
    		recurrence = Recurrence.ONE_OFF;
    		jobCostingExpression = serviceCostingExpression.get();
    	} 
    	else {
    		recurrence = Recurrence.HOURLY;
    		jobCostingExpression = defaultJobCostingExpression;
    	}
    	
        String expression = jobCostingExpression.getCostExpression();
        int cost = ((Number) expressionParser.parseExpression(expression).getValue(job)).intValue();
        return new CostQuotation(cost, recurrence);
    }

	@Override
    @Transactional
    public void chargeForDownload(Wallet wallet, FstepFile fstepFile) {
        CostingExpression costingExpression = getCostingExpression(fstepFile);

        String expression = costingExpression.getCostExpression();

        int cost = ((Number) expressionParser.parseExpression(expression).getValue(fstepFile)).intValue();
        WalletTransaction walletTransaction = WalletTransaction.builder()
                .wallet(walletDataService.refreshFull(wallet))
                .balanceChange(-cost)
                .type(WalletTransaction.Type.DOWNLOAD)
                .associatedId(fstepFile.getId())
                .transactionTime(LocalDateTime.now())
                .build();
        walletDataService.transact(walletTransaction);
    }

    private CostingExpression getCostingExpression(FstepFile fstepFile) {
        return Optional.ofNullable(costingDataService.getDownloadCostingExpression(fstepFile)).orElse(defaultDownloadCostingExpression);
    }

	@Override
	@Transactional
	public void chargeForSubscription(Wallet wallet, Subscription subscription) {
		int cost = subscription.getQuantity() * subscription.getSubscriptionPlan().getCostQuotation().getCost();
        WalletTransaction walletTransaction = WalletTransaction.builder()
                .wallet(walletDataService.refreshFull(wallet))
                .balanceChange(-cost)
                .type(WalletTransaction.Type.SUBSCRIPTION)
                .associatedId(subscription.getId())
                .transactionTime(LocalDateTime.now())
                .build();
        walletDataService.transact(walletTransaction);
		
	}

	@Override
	@Transactional
	public void transactForSubscription(Wallet wallet, Subscription subscription, int amount) {
		WalletTransaction walletTransaction = WalletTransaction.builder()
                .wallet(walletDataService.refreshFull(wallet))
                .balanceChange(amount)
                .type(WalletTransaction.Type.SUBSCRIPTION)
                .associatedId(subscription.getId())
                .transactionTime(LocalDateTime.now())
                .build();
        walletDataService.transact(walletTransaction);
		
	}

}
