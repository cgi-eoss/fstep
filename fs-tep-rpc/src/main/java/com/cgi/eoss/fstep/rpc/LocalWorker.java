package com.cgi.eoss.fstep.rpc;


import com.cgi.eoss.fstep.rpc.worker.ContainerExitCode;
import com.cgi.eoss.fstep.rpc.worker.ExitParams;
import com.cgi.eoss.fstep.rpc.worker.ExitWithTimeoutParams;
import com.cgi.eoss.fstep.rpc.worker.FstepWorkerGrpc;
import com.cgi.eoss.fstep.rpc.worker.JobDockerConfig;
import com.cgi.eoss.fstep.rpc.worker.JobEnvironment;
import com.cgi.eoss.fstep.rpc.worker.JobInputs;
import com.cgi.eoss.fstep.rpc.worker.LaunchContainerResponse;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

public class LocalWorker {
    private final ManagedChannelBuilder inProcessChannelBuilder;

    public LocalWorker(ManagedChannelBuilder inProcessChannelBuilder) {
        this.inProcessChannelBuilder = inProcessChannelBuilder;
    }
    
    public JobEnvironment prepareInputs(JobInputs request) {
        FstepWorkerGrpc.FstepWorkerBlockingStub worker = FstepWorkerGrpc.newBlockingStub(inProcessChannelBuilder.build());
        return worker.prepareInputs(request);
    }

    public LaunchContainerResponse launchContainer(JobDockerConfig request) {
        FstepWorkerGrpc.FstepWorkerBlockingStub worker = FstepWorkerGrpc.newBlockingStub(inProcessChannelBuilder.build());
        return worker.launchContainer(request);
    }

    public ContainerExitCode waitForContainerExitWithTimeout(ExitWithTimeoutParams request) {
        FstepWorkerGrpc.FstepWorkerBlockingStub worker = FstepWorkerGrpc.newBlockingStub(inProcessChannelBuilder.build());
        return worker.waitForContainerExitWithTimeout(request);
   }

    public ContainerExitCode waitForContainerExit(ExitParams request) {
        FstepWorkerGrpc.FstepWorkerBlockingStub worker = FstepWorkerGrpc.newBlockingStub(inProcessChannelBuilder.build());
        return worker.waitForContainerExit(request);
    }

   
}
