package com.cgi.eoss.fstep.rpc;


import org.springframework.scheduling.annotation.Async;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

public class LocalServiceLauncher {
    private final ManagedChannelBuilder inProcessChannelBuilder;

    public LocalServiceLauncher(ManagedChannelBuilder inProcessChannelBuilder) {
        this.inProcessChannelBuilder = inProcessChannelBuilder;
    }
    
    @Async
    public void asyncSubmitJob(FstepServiceParams serviceParams, StreamObserver<FstepServiceResponse> responseObserver) {
        FstepJobLauncherGrpc.FstepJobLauncherStub jobLauncher = FstepJobLauncherGrpc.newStub(inProcessChannelBuilder.build());
        jobLauncher.submitJob(serviceParams, responseObserver);
    }

    @Async
    public void asyncLaunchService(FstepServiceParams serviceParams, StreamObserver<FstepServiceResponse> responseObserver) {
        FstepServiceLauncherGrpc.FstepServiceLauncherStub serviceLauncher = FstepServiceLauncherGrpc.newStub(inProcessChannelBuilder.build());
        serviceLauncher.launchService(serviceParams, responseObserver);
    }
    
    @Async
    public void asyncStopService(StopServiceParams stopServiceParams, StreamObserver<StopServiceResponse> responseObserver) {
        FstepServiceLauncherGrpc.FstepServiceLauncherStub serviceLauncher = FstepServiceLauncherGrpc.newStub(inProcessChannelBuilder.build());
        serviceLauncher.stopService(stopServiceParams, responseObserver);
    }
    
    @Async
    public void asyncCancelJob(CancelJobParams cancelJobParams, StreamObserver<CancelJobResponse> responseObserver) {
        FstepJobLauncherGrpc.FstepJobLauncherStub jobLauncher = FstepJobLauncherGrpc.newStub(inProcessChannelBuilder.build());
        jobLauncher.cancelJob(cancelJobParams, responseObserver);
    }
    
    @Async
    public void asyncStopJob(StopServiceParams stopServiceParams, StreamObserver<StopServiceResponse> responseObserver) {
        FstepJobLauncherGrpc.FstepJobLauncherStub jobLauncher = FstepJobLauncherGrpc.newStub(inProcessChannelBuilder.build());
        jobLauncher.stopJob(stopServiceParams, responseObserver);
    }
}
