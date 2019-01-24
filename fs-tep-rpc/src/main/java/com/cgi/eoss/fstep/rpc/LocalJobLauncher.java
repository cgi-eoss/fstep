package com.cgi.eoss.fstep.rpc;


import org.springframework.scheduling.annotation.Async;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

public class LocalJobLauncher {
    private final ManagedChannelBuilder inProcessChannelBuilder;

    public LocalJobLauncher(ManagedChannelBuilder inProcessChannelBuilder) {
        this.inProcessChannelBuilder = inProcessChannelBuilder;
    }
    
    @Async
    public void asyncSubmitJob(FstepServiceParams serviceParams, StreamObserver<FstepJobResponse> responseObserver) {
        FstepJobLauncherGrpc.FstepJobLauncherStub jobLauncher = FstepJobLauncherGrpc.newStub(inProcessChannelBuilder.build());
        jobLauncher.submitJob(serviceParams, responseObserver);
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

    @Async
    public void asyncRelaunchFailedJob(RelaunchFailedJobParams relaunchJobParams, StreamObserver<RelaunchFailedJobResponse> responseObserver) {
        FstepJobLauncherGrpc.FstepJobLauncherStub jobLauncher = FstepJobLauncherGrpc.newStub(inProcessChannelBuilder.build());
        jobLauncher.relaunchFailedJob(relaunchJobParams, responseObserver);
    }
    
    @Async
    public void asyncBuildService(BuildServiceParams buildServiceParams, StreamObserver<BuildServiceResponse> responseObserver) {
        FstepJobLauncherGrpc.FstepJobLauncherStub jobLauncher = FstepJobLauncherGrpc.newStub(inProcessChannelBuilder.build());
        jobLauncher.buildService(buildServiceParams, responseObserver);
    }
}
