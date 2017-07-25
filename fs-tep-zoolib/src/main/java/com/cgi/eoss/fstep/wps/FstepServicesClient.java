package com.cgi.eoss.fstep.wps;

import com.cgi.eoss.fstep.rpc.FstepServiceLauncherGrpc;
import com.cgi.eoss.fstep.rpc.FstepServiceParams;
import com.cgi.eoss.fstep.rpc.FstepServiceResponse;
import com.cgi.eoss.fstep.rpc.GrpcUtil;
import com.cgi.eoss.fstep.rpc.Job;
import com.google.common.base.Joiner;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.CloseableThreadContext;
import org.zoo_project.ZOO;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * <p>Client for FS-TEP gRPC services. Encapsulates the usage of the RPC interface so that WPS service implementations
 * may access the FS-TEP orchestration environment more easily.</p>
 */
@Log4j2
public class FstepServicesClient {

    private final ManagedChannel channel;
    private final FstepServiceLauncherGrpc.FstepServiceLauncherBlockingStub fstepServiceLauncherBlockingStub;
    private final FstepServiceLauncherGrpc.FstepServiceLauncherStub fstepServiceLauncherStub;

    /**
     * <p>Construct gRPC client connecting to server at ${host}:${port}.</p>
     */
    public FstepServicesClient(String host, int port) {
        // TLS is unused since this should only be active in the FS-TEP infrastructure, i.e. not public
        // TODO Investigate feasibility of certificate security
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext(true));
    }

    /**
     * <p>Construct gRPC client using an existing channel builder.</p>
     */
    public FstepServicesClient(ManagedChannelBuilder<?> channelBuilder) {
        channel = channelBuilder.build();
        fstepServiceLauncherBlockingStub = FstepServiceLauncherGrpc.newBlockingStub(channel);
        fstepServiceLauncherStub = FstepServiceLauncherGrpc.newStub(channel);
    }

    /**
     * <p>Tear down gRPC channel safely.</p>
     *
     * @throws InterruptedException
     */
    @PreDestroy
    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    /**
     * <p>Launch a WPS Processor service with a blocking call.</p>
     *
     * @param userId The ID of the user launching the service.
     * @param serviceId The ID of the service being launched. Used to determine the application container to use.
     * @param jobId The job ID as set by the WPS server.
     * @param inputs The WPS parameter inputs. Expected to be a keyed list of strings.
     */
    public Multimap<String, String> launchService(String userId, String serviceId, String jobId, Multimap<String, String> inputs) {
        FstepServiceParams request = FstepServiceParams.newBuilder()
                .setJobId(jobId)
                .setUserId(userId)
                .setServiceId(serviceId)
                .addAllInputs(GrpcUtil.mapToParams(inputs))
                .build();
        Iterator<FstepServiceResponse> responseIterator = fstepServiceLauncherBlockingStub.launchService(request);

        // First message is the persisted job metadata
        Job jobInfo = responseIterator.next().getJob();
        LOG.info("Instantiated job: {}", jobInfo);

        // Second message is the outputs
        FstepServiceResponse.JobOutputs jobOutputs = responseIterator.next().getJobOutputs();
        return GrpcUtil.paramsListToMap(jobOutputs.getOutputsList());
    }

    /**
     * <p>Entry point for WPS services to hook into the FS-TEP service launching infrastructure.</p>
     */
    @SuppressWarnings("unchecked")
    public static int launch(String serviceId, HashMap conf, HashMap inputs, HashMap outputs) {
        try (CloseableThreadContext.Instance ctc = CloseableThreadContext.push("ZOO entry point")) {
            Map<String, String> fstepConf = (Map<String, String>) conf.get("fstep");

            String jobIdKey = fstepConf.getOrDefault("zooConfKeyJobId", "uusid");
            String usernameHeaderKey = fstepConf.getOrDefault("zooConfKeyUsernameHeader", "HTTP_REMOTE_USER");

            String jobId = (String) ((HashMap) conf.get("lenv")).get(jobIdKey);
            String userId = (String) ((HashMap) conf.get("renv")).get(usernameHeaderKey);

            ctc.put("userId", userId);
            ctc.put("serviceId", serviceId);
            ctc.put("zooId", jobId);

            LOG.info("FS-TEP service requested by user {}: {}, jobId: {}", userId, serviceId, jobId);

            String fstepServerHost = fstepConf.getOrDefault("fstepServerGrpcHost", "localhost");
            Integer fstepServerPort = Integer.parseInt(fstepConf.getOrDefault("fstepServerGrpcPort", "6565"));

            LOG.debug("ZOO service connecting to FS-TEP Server located at: {}:{}", fstepServerHost, fstepServerPort);

            Multimap<String, String> inputParams = MultimapBuilder.hashKeys(inputs.size()).hashSetValues().build();

            for (Map.Entry<String, HashMap<String, Object>> input : ((Map<String, HashMap<String, Object>>) inputs).entrySet()) {
                if (input.getValue().containsKey("isArray")) {
                    inputParams.putAll(input.getKey(), (ArrayList<String>) input.getValue().get("value"));
                } else {
                    inputParams.put(input.getKey(), (String) input.getValue().get("value"));
                }
            }

            FstepServicesClient client = new FstepServicesClient(fstepServerHost, fstepServerPort);

            // TODO Translate fstep:// internal URLs for external users
            Multimap<String, String> result = client.launchService(userId, serviceId, jobId, inputParams);

            LOG.info("Received result for job {}: {}", jobId, result);

            Multimaps.asMap(result).forEach((k, v) -> outputs.put(k, Joiner.on(',').join(v)));
            return ZOO.SERVICE_SUCCEEDED;
        } catch (Exception e) {
            LOG.error("Service execution failed", e);
            return ZOO.SERVICE_FAILED;
        }
    }

}
