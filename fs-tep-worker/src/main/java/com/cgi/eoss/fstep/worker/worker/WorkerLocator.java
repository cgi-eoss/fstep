package com.cgi.eoss.fstep.worker.worker;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import com.cgi.eoss.fstep.rpc.worker.FstepWorkerGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.log4j.Log4j2;

/**
 * <p>Service providing access to FS-TEP Worker nodes based on environment requests.</p>
 */
@Log4j2
public class WorkerLocator {

    private final DiscoveryClient discoveryClient;
    private String workerServiceId;
    
    public WorkerLocator(DiscoveryClient discoveryClient, String workerServiceId) {
        this.discoveryClient = discoveryClient;
        this.workerServiceId = workerServiceId;
    }
    
    /**
     * @return The worker with the specified id
     */
    public FstepWorkerGrpc.FstepWorkerBlockingStub getWorkerById(String workerId) {
        LOG.debug("Locating worker with id {}", workerId);
        ServiceInstance worker = discoveryClient.getInstances(workerServiceId).stream()
                .filter(si -> si.getMetadata().get("workerId").equals(workerId))
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException("Unable to find worker with id: " + workerId));

        LOG.info("Located {} worker: {}:{}", workerId, worker.getHost(), worker.getMetadata().get("grpcPort"));

        ManagedChannel managedChannel = ManagedChannelBuilder.forAddress(worker.getHost(), Integer.parseInt(worker.getMetadata().get("grpcPort")))
                .usePlaintext(true)
                .build();

        return FstepWorkerGrpc.newBlockingStub(managedChannel);
    }

}
