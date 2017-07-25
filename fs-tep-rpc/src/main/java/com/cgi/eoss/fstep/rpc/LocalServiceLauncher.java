package com.cgi.eoss.fstep.rpc;


import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.springframework.scheduling.annotation.Async;

public class LocalServiceLauncher {
    private final ManagedChannelBuilder inProcessChannelBuilder;

    public LocalServiceLauncher(ManagedChannelBuilder inProcessChannelBuilder) {
        this.inProcessChannelBuilder = inProcessChannelBuilder;
    }

    @Async
    public void asyncLaunchService(FstepServiceParams serviceParams, StreamObserver<FstepServiceResponse> responseObserver) {
        FstepServiceLauncherGrpc.FstepServiceLauncherStub serviceLauncher = FstepServiceLauncherGrpc.newStub(inProcessChannelBuilder.build());
        serviceLauncher.launchService(serviceParams, responseObserver);
    }
}
