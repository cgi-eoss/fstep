package com.cgi.eoss.fstep.orchestrator.service;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.geojson.Feature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.cgi.eoss.fstep.costing.CostingService;
import com.cgi.eoss.fstep.model.CostQuotation;
import com.cgi.eoss.fstep.model.JobConfig;
import com.cgi.eoss.fstep.model.SystematicProcessing;
import com.cgi.eoss.fstep.model.SystematicProcessing.Status;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.persistence.service.SystematicProcessingDataService;
import com.cgi.eoss.fstep.rpc.FstepServiceParams;
import com.cgi.eoss.fstep.rpc.GrpcUtil;
import com.cgi.eoss.fstep.rpc.LocalJobLauncher;
import com.cgi.eoss.fstep.search.api.SearchFacade;
import com.cgi.eoss.fstep.search.api.SearchParameters;
import com.cgi.eoss.fstep.search.api.SearchResults;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;

import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;

/**
 * <p>
 * Service to submit systematic jobs
 * </p>
 */
@Log4j2
@Service
@ConditionalOnProperty(name="fstep.server.systematicscheduler.enabled", havingValue="true", matchIfMissing = false)
public class SystematicProcessingScheduler {

    SearchFacade searchFacade;
    SystematicProcessingDataService systematicProcessingDataService;
    private LocalJobLauncher localJobLauncher;
    private CostingService costingService;

    private static final long SYSTEMATIC_PROCESSING_CHECK_RATE_MS = 60 * 60 * 1000L;

    @Autowired
    public SystematicProcessingScheduler(SystematicProcessingDataService systematicProcessingDataService, SearchFacade searchFacade,
            LocalJobLauncher localJobLauncher, CostingService costingService) {
        this.systematicProcessingDataService = systematicProcessingDataService;
        this.searchFacade = searchFacade;
        this.localJobLauncher = localJobLauncher;
        this.costingService = costingService;
    }

    @Scheduled(fixedRate = SYSTEMATIC_PROCESSING_CHECK_RATE_MS, initialDelay = 10000L)
    public void updateSystematicProcessings() {
        List<SystematicProcessing> activeSystematicProcessings = systematicProcessingDataService.findByStatus(Status.ACTIVE);
        List<SystematicProcessing> blockedSystematicProcessings = systematicProcessingDataService.findByStatus(Status.BLOCKED);
        
        for (SystematicProcessing activeSystematicProcessing : activeSystematicProcessings) {
            updateSystematicProcessing(activeSystematicProcessing);
        }
        
        //Try to resume blocked systematic processings
        for (SystematicProcessing blockedSystematicProcessing : blockedSystematicProcessings) {
            User user = blockedSystematicProcessing.getOwner();
            if (user.getWallet().getBalance() > 0) {
                blockedSystematicProcessing.setStatus(Status.ACTIVE);
                systematicProcessingDataService.save(blockedSystematicProcessing);
                updateSystematicProcessing(blockedSystematicProcessing);
            }
        }
    }

    public void updateSystematicProcessing(SystematicProcessing systematicProcessing) {
        ListMultimap<String, String> queryParameters = systematicProcessing.getSearchParameters();
        queryParameters.replaceValues("publishedAfter", Arrays.asList(new String[] {
                ZonedDateTime.of(systematicProcessing.getLastUpdated(), ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}));
        queryParameters.replaceValues("publishedBefore", Arrays.asList(new String[] {ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}));
        queryParameters.replaceValues("sortOrder", Arrays.asList(new String[] {"ascending"}));
        queryParameters.replaceValues("sortParam", Arrays.asList(new String[] {"published"}));
        int page = 0;
        HttpUrl requestUrl = new HttpUrl.Builder().scheme("http").host("local").build();
        SearchResults results = null;
        JobConfig configTemplate = systematicProcessing.getParentJob().getConfig();
        try {
            do {
                results = getSearchResultsPage(requestUrl, page, queryParameters);
                for (Feature feature : results.getFeatures()) {
                    String url = feature.getProperties().get("fstepUrl").toString();
                    Multimap<String, String> inputs = ArrayListMultimap.create();
                    inputs.putAll(configTemplate.getInputs());
                    inputs.replaceValues(configTemplate.getSystematicParameter(), Arrays.asList(new String[] {url}));
                    configTemplate.getInputs().put(configTemplate.getSystematicParameter(), url);
                    CostQuotation jobCostEstimate = costingService.estimateSingleRunJobCost(configTemplate);
                    if (jobCostEstimate.getCost() > systematicProcessing.getOwner().getWallet().getBalance()) {
                        systematicProcessing.setStatus(Status.BLOCKED);
                        systematicProcessingDataService.save(systematicProcessing);
                        return;
                    }
                    submitJob(configTemplate.getOwner().getName(), configTemplate.getService().getName(), String.valueOf(systematicProcessing.getParentJob().getId()), inputs);
                    Map<String, Object> extraParams = (Map<String, Object>) feature.getProperties().get("extraParams");
                    systematicProcessing
                            .setLastUpdated(ZonedDateTime.parse(extraParams.get("fstepUpdated").toString()).toLocalDateTime().plusSeconds(1));
                }
                page++;
            } while (results.getLinks().containsKey("next"));
        } catch (IOException e) {
            LOG.error("Failure running search for systematic processing {}", systematicProcessing.getId());
        } catch (InterruptedException e) {
            LOG.error("Failure submitting job for systematic processing {}", systematicProcessing.getId());
        } catch (JobSubmissionException e) {
            LOG.error("Failure submitting job for systematic processing {} ", systematicProcessing.getId());
            systematicProcessing.setStatus(Status.BLOCKED);
        } finally {
            systematicProcessingDataService.save(systematicProcessing);
        }
    }

    private SearchResults getSearchResultsPage(HttpUrl requestUrl, int page, ListMultimap<String, String> queryParameters)
            throws IOException {
        SearchParameters sp = new SearchParameters();
        sp.setPage(page);
        sp.setResultsPerPage(20);
        sp.setRequestUrl(requestUrl);
        sp.setParameters(queryParameters);
        return searchFacade.search(sp);

    }

    private void submitJob(String userName, String serviceName, String parentId, Multimap<String, String> inputs) throws InterruptedException, JobSubmissionException {
        FstepServiceParams.Builder serviceParamsBuilder =
                FstepServiceParams.newBuilder().setJobId(UUID.randomUUID().toString()).setUserId(userName)
                        .setJobParent(parentId)
                        .setServiceId(serviceName).addAllInputs(GrpcUtil.mapToParams(inputs));

        try {
        	localJobLauncher.syncSubmitJob(serviceParamsBuilder.build());
        }
        catch (Exception e) {
            throw new JobSubmissionException(e);
        } 
    }

    public class JobSubmissionException extends Exception {

        public JobSubmissionException(Throwable t) {
            super(t);
        }

    }

}
