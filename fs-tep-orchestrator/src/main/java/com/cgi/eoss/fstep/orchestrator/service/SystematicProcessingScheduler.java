package com.cgi.eoss.fstep.orchestrator.service;

import com.cgi.eoss.fstep.costing.CostingService;
import com.cgi.eoss.fstep.model.JobConfig;
import com.cgi.eoss.fstep.model.SystematicProcessing;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.model.SystematicProcessing.Status;
import com.cgi.eoss.fstep.persistence.service.SystematicProcessingDataService;
import com.cgi.eoss.fstep.rpc.FstepServiceParams;
import com.cgi.eoss.fstep.rpc.FstepServiceResponse;
import com.cgi.eoss.fstep.rpc.GrpcUtil;
import com.cgi.eoss.fstep.rpc.LocalServiceLauncher;
import com.cgi.eoss.fstep.search.api.SearchFacade;
import com.cgi.eoss.fstep.search.api.SearchParameters;
import com.cgi.eoss.fstep.search.api.SearchResults;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import io.grpc.stub.StreamObserver;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import org.geojson.Feature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Service for autoscaling the number of worker nodes based on queue length
 * </p>
 */
@Log4j2
@Service
@ConditionalOnProperty(name="fstep.server.systematicscheduler.enabled", havingValue="true", matchIfMissing = false)
public class SystematicProcessingScheduler {

    SearchFacade searchFacade;
    SystematicProcessingDataService systematicProcessingDataService;
    private LocalServiceLauncher localServiceLauncher;
    private CostingService costingService;

    private static final long SYSTEMATIC_PROCESSING_CHECK_RATE_MS = 60 * 60 * 1000;

    @Autowired
    public SystematicProcessingScheduler(SystematicProcessingDataService systematicProcessingDataService, SearchFacade searchFacade,
            LocalServiceLauncher localServiceLauncher, CostingService costingService) {
        this.systematicProcessingDataService = systematicProcessingDataService;
        this.searchFacade = searchFacade;
        this.localServiceLauncher = localServiceLauncher;
        this.costingService = costingService;
    }

    @Scheduled(fixedRate = SYSTEMATIC_PROCESSING_CHECK_RATE_MS, initialDelay = 10000L)
    public void updateSystematicProcessings() {
        List<SystematicProcessing> activeSystematicProcessings = systematicProcessingDataService.findByStatus(Status.ACTIVE);
        for (SystematicProcessing activeSystematicProcessing : activeSystematicProcessings) {
            updateSystematicProcessing(activeSystematicProcessing);
        }
        
        //Try to resume blocked systematic processings
        List<SystematicProcessing> blockedSystematicProcessings = systematicProcessingDataService.findByStatus(Status.BLOCKED);
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
        queryParameters.replaceValues("sortParam", Arrays.asList(new String[] {"updated"}));
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
                    int jobCost = costingService.estimateSingleRunJobCost(configTemplate);
                    if (jobCost > systematicProcessing.getOwner().getWallet().getBalance()) {
                        systematicProcessing.setStatus(Status.BLOCKED);
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

        final CountDownLatch latch = new CountDownLatch(1);
        JobLaunchObserver responseObserver = new JobLaunchObserver(latch);
        localServiceLauncher.asyncSubmitJob(serviceParamsBuilder.build(), responseObserver);
        // Block until the latch counts down (i.e. one message from the server)
        latch.await(1, TimeUnit.MINUTES);
        if (responseObserver.getError() != null) {
            throw new JobSubmissionException(responseObserver.getError());
        } 
    }

    private static final class JobLaunchObserver implements StreamObserver<FstepServiceResponse> {

        private final CountDownLatch latch;
        @Getter
        private long intJobId;

        @Getter
        private Throwable error;

        JobLaunchObserver(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void onNext(FstepServiceResponse value) {
            if (value.getPayloadCase() == FstepServiceResponse.PayloadCase.JOB) {
                this.intJobId = Long.parseLong(value.getJob().getIntJobId());
                LOG.info("Received job ID: {}", this.intJobId);
            }
            latch.countDown();
        }

        @Override
        public void onError(Throwable t) {
            error = t;

        }

        @Override
        public void onCompleted() {
            // No-op, the user has long stopped listening here
        }
    }

    public class JobSubmissionException extends Exception {

        public JobSubmissionException(Throwable t) {
            super(t);
        }

    }

}
