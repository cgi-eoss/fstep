package com.cgi.eoss.fstep.api.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.text.StrSubstitutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.cgi.eoss.fstep.model.Job;
import com.cgi.eoss.fstep.orchestrator.utils.ModelToGrpcUtils;
import com.cgi.eoss.fstep.rpc.CancelJobParams;
import com.cgi.eoss.fstep.rpc.CancelJobResponse;
import com.cgi.eoss.fstep.rpc.IngestJobOutputsParams;
import com.cgi.eoss.fstep.rpc.IngestJobOutputsResponse;
import com.cgi.eoss.fstep.rpc.LocalJobLauncher;
import com.cgi.eoss.fstep.rpc.RelaunchFailedJobParams;
import com.cgi.eoss.fstep.rpc.RelaunchFailedJobResponse;
import com.cgi.eoss.fstep.rpc.StopServiceParams;
import com.cgi.eoss.fstep.rpc.StopServiceResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

import io.grpc.stub.StreamObserver;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

@RestController
@BasePathAwareController
@RequestMapping("/jobs")
@Transactional
@Log4j2
public class JobsApiExtension {

    // TODO Make configurable
    @Value("${fstep.api.logs.graylogApiQuery:contextStack%3A%22%5BIn-Docker%2C%20FS-TEP%20Worker%5D%22%20AND%20zooId%3A@{zooId}}")
    private String dockerJobLogQuery;

    @Value("${fstep.api.logs.graylogApiUrl:http://fstep-monitor:8087/log/api}")
    private String graylogApiUrl;

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final LocalJobLauncher localJobLauncher;

    @Autowired
    public JobsApiExtension(@Value("${fstep.api.logs.username:admin}") String username,
                            @Value("${fstep.api.logs.password:graylogpass}") String password,
                            LocalJobLauncher localJobLauncher) {
        this.httpClient = new OkHttpClient.Builder()
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request request = chain.request();
                        Request authenticatedRequest = request.newBuilder()
                                .header("Authorization", Credentials.basic(username, password))
                                .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                                .build();
                        return chain.proceed(authenticatedRequest);
                    }
                })
                .addInterceptor(new HttpLoggingInterceptor(LOG::trace).setLevel(HttpLoggingInterceptor.Level.BODY))
                .build();
        this.objectMapper = new ObjectMapper();
        this.localJobLauncher = localJobLauncher;
    }

    @GetMapping("/{jobId}/logs")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#job, 'read')")
    @ResponseBody
    public List<SimpleMessage> getJobContainerLogs(@ModelAttribute("jobId") Job job) throws IOException {
        Map<String, String> parameters = ImmutableMap.<String, String>builder()
                .put("range", "0")
                .put("sort", "timestamp%3Aasc")
                .put("decorate", "false")
                .put("stored_fields", "timestamp%2Cmessage")
                .put("query", StrSubstitutor.replace(dockerJobLogQuery, ImmutableMap.of("zooId", job.getExtId()), "@{", "}"))
                .build();

        HttpUrl.Builder urlBuilder = HttpUrl.parse(graylogApiUrl).newBuilder().addPathSegments("search/universal/relative");
        parameters.forEach(urlBuilder::addEncodedQueryParameter);
        HttpUrl searchUrl = urlBuilder.build();

        List<SimpleMessage> messages = new ArrayList<>();
        LOG.debug("Retrieving job {} logs from url: {}", job.getId(), searchUrl);
        loadGraylogMessages(messages, searchUrl);
        return messages;
    }

    private void loadGraylogMessages(List<SimpleMessage> messages, HttpUrl graylogApiUrl) throws IOException {
        Request request = new Request.Builder()
                .get()
                .url(graylogApiUrl)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                GraylogApiResponse graylogApiResponse = objectMapper.readValue(response.body().string(), GraylogApiResponse.class);
                graylogApiResponse.getMessages().stream()
                        .map(GraylogMessage::getMessage)
                        .forEach(messages::add);

                if (messages.size() < graylogApiResponse.getTotalResults()) {
                    loadGraylogMessages(messages, graylogApiUrl.newBuilder().setQueryParameter("offset", String.valueOf(messages.size())).build());
                }
            } else {
                LOG.error("Failed to retrieve logs: {} -- {}", response.code(), response.message());
                LOG.debug("Graylog response: {}", response.body());
            }
        }
    }

    @PostMapping("/{jobId}/terminate")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#job, 'write')")
    public ResponseEntity stop(@ModelAttribute("jobId") Job job) throws InterruptedException {
        StopServiceParams stopServiceParams = StopServiceParams.newBuilder()
                .setJob(ModelToGrpcUtils.toRpcJob(job))
                .build();

        final CountDownLatch latch = new CountDownLatch(1);
        JobStopObserver responseObserver = new JobStopObserver(latch);

        localJobLauncher.asyncStopJob(stopServiceParams, responseObserver);

        latch.await(1, TimeUnit.MINUTES);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/{jobId}/cancel")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#job, 'write')")
    @ResponseBody
    public void cancelJob(@ModelAttribute("jobId") Job job) throws IOException {
        CancelJobParams.Builder cancelJobParamsBuilder = CancelJobParams.newBuilder()
                .setJob(ModelToGrpcUtils.toRpcJob(job));

        CancelJobParams cancelJobParams = cancelJobParamsBuilder.build();

        LOG.info("Cancelling job via REST API: {}", cancelJobParams);
        
        final CountDownLatch latch = new CountDownLatch(1);
        JobCancelObserver responseObserver = new JobCancelObserver(latch);

        localJobLauncher.asyncCancelJob(cancelJobParams, responseObserver);
        
    }
    
    @PostMapping("/{jobId}/relaunchFailed")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#job, 'write')")
    @ResponseBody
    public void relaunchFailedJob(@ModelAttribute("jobId") Job job) throws IOException {
        RelaunchFailedJobParams.Builder relaunchJobParamsBuilder = RelaunchFailedJobParams.newBuilder()
                .setJob(ModelToGrpcUtils.toRpcJob(job));
        
        RelaunchFailedJobParams relaunchJobParams = relaunchJobParamsBuilder.build();

        LOG.info("Relaunching failed job via REST API: {}", relaunchJobParams);
        
        final CountDownLatch latch = new CountDownLatch(1);
        JobRelaunchObserver responseObserver = new JobRelaunchObserver(latch);

        localJobLauncher.asyncRelaunchFailedJob(relaunchJobParams, responseObserver);
        
    }
    
    @PostMapping("/{jobId}/ingestOutputs")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#job, 'write')")
    @ResponseBody
    public void ingestJobOutputs(@ModelAttribute("jobId") Job job) throws IOException {
        IngestJobOutputsParams.Builder ingestJobOutputsParamsBuilder = IngestJobOutputsParams.newBuilder()
                .setJob(ModelToGrpcUtils.toRpcJob(job));
        
        IngestJobOutputsParams ingestJobOutputsParams = ingestJobOutputsParamsBuilder.build();

        LOG.info("Ingesting job outputs via REST API: {}", ingestJobOutputsParams);
        
        IngestJobOutputsObserver ingestJobOutputsObserver = new IngestJobOutputsObserver();
        
        localJobLauncher.asyncIngestJobOutputs(ingestJobOutputsParams, ingestJobOutputsObserver);
        
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class GraylogApiResponse {
        private List<GraylogMessage> messages;
        @JsonProperty("total_results")
        private Long totalResults;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class GraylogMessage {
        private SimpleMessage message;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class SimpleMessage {
        private String timestamp;
        private String message;
    }

    private static final class JobStopObserver implements StreamObserver<StopServiceResponse> {
        private final CountDownLatch latch;

        JobStopObserver(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void onNext(StopServiceResponse value) {
            LOG.debug("Received StopServiceResponse: {}", value);
            latch.countDown();
        }

        @Override
        public void onError(Throwable t) {
            LOG.error("Failed to stop service via REST API", t);
        }

        @Override
        public void onCompleted() {
            // No-op, the user has long stopped listening here
        }
    }
    
    private static final class JobCancelObserver implements StreamObserver<CancelJobResponse> {
        private final CountDownLatch latch;

        JobCancelObserver(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void onNext(CancelJobResponse value) {
            LOG.debug("Received CancelJobResponse: {}", value);
            latch.countDown();
        }

        @Override
        public void onError(Throwable t) {
            LOG.error("Failed to cancel job via REST API", t);
        }

        @Override
        public void onCompleted() {
            // No-op, the user has long stopped listening here
        }
    }
    
    private static final class JobRelaunchObserver implements StreamObserver<RelaunchFailedJobResponse> {
        private final CountDownLatch latch;

        JobRelaunchObserver(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void onNext(RelaunchFailedJobResponse value) {
            LOG.debug("Received RealunchFailedJobResponse: {}", value);
            latch.countDown();
        }

        @Override
        public void onError(Throwable t) {
            LOG.error("Failed to relaunch job via REST API", t);
        }

        @Override
        public void onCompleted() {
            // No-op, the user has long stopped listening here
        }
    }
    
    private static final class IngestJobOutputsObserver implements StreamObserver<IngestJobOutputsResponse> {
        
       
        @Override
        public void onNext(IngestJobOutputsResponse value) {
            LOG.debug("Received IngestJobOutputsResponse: {}", value);
           
        }

        @Override
        public void onError(Throwable t) {
            LOG.error("Failed to ingest job output via REST API", t);
        }

        @Override
        public void onCompleted() {
            // No-op, the user has long stopped listening here
        }
    }

}