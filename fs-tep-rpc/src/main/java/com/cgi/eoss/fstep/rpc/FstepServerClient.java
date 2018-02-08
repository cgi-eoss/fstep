package com.cgi.eoss.fstep.rpc;

import com.cgi.eoss.fstep.rpc.catalogue.CatalogueServiceGrpc;
import com.google.common.collect.Iterables;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

public class FstepServerClient {
    private final DiscoveryClient discoveryClient;
    private final String fstepServerServiceId;

    public FstepServerClient(DiscoveryClient discoveryClient, String fstepServerServiceId) {
        this.discoveryClient = discoveryClient;
        this.fstepServerServiceId = fstepServerServiceId;
    }

    public ServiceContextFilesServiceGrpc.ServiceContextFilesServiceBlockingStub serviceContextFilesServiceBlockingStub() {
        return ServiceContextFilesServiceGrpc.newBlockingStub(getChannel());
    }

    public CredentialsServiceGrpc.CredentialsServiceBlockingStub credentialsServiceBlockingStub() {
        return CredentialsServiceGrpc.newBlockingStub(getChannel());
    }

    public CatalogueServiceGrpc.CatalogueServiceBlockingStub catalogueServiceBlockingStub() {
        return CatalogueServiceGrpc.newBlockingStub(getChannel());
    }
    
    public CatalogueServiceGrpc.CatalogueServiceStub catalogueServiceStub() {
        return CatalogueServiceGrpc.newStub(getChannel());
    }

    private ManagedChannel getChannel() {
        ServiceInstance fstepServer = Iterables.getOnlyElement(discoveryClient.getInstances(fstepServerServiceId));

        return ManagedChannelBuilder.forAddress(fstepServer.getHost(), Integer.parseInt(fstepServer.getMetadata().get("grpcPort")))
                .usePlaintext(true)
                .build();
    }

}
