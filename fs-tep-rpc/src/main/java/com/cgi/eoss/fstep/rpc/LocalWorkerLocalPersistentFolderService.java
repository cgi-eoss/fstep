package com.cgi.eoss.fstep.rpc;


import org.springframework.scheduling.annotation.Async;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

public class LocalWorkerLocalPersistentFolderService {
    private final ManagedChannelBuilder inProcessChannelBuilder;

    public LocalWorkerLocalPersistentFolderService(ManagedChannelBuilder inProcessChannelBuilder) {
        this.inProcessChannelBuilder = inProcessChannelBuilder;
    }
    
    @Async
    public void asyncCreateUserPersistentFolder(WorkerLocalPersistentFolderParams request, StreamObserver<PersistentFolderCreationResponse> responseObserver) {
        WorkerLocalPersistentFolderServiceGrpc.WorkerLocalPersistentFolderServiceStub persistentFolderService = WorkerLocalPersistentFolderServiceGrpc.newStub(inProcessChannelBuilder.build());
        persistentFolderService.createUserPersistentFolder(request, responseObserver);
    }
    
    @Async
    public void asyncSetUserPersistentFolderQuota(WorkerLocalPersistentFolderParams request, StreamObserver<PersistentFolderSetQuotaResponse> responseObserver) {
        WorkerLocalPersistentFolderServiceGrpc.WorkerLocalPersistentFolderServiceStub persistentFolderService = WorkerLocalPersistentFolderServiceGrpc.newStub(inProcessChannelBuilder.build());
        persistentFolderService.setUserPersistentFolderQuota(request, responseObserver);
    }
    
    @Async
    public void asyncGetUserPersistentFolderUsage(WorkerLocalPersistentFolderUsageParams request, StreamObserver<PersistentFolderUsageResponse> responseObserver) {
        WorkerLocalPersistentFolderServiceGrpc.WorkerLocalPersistentFolderServiceStub persistentFolderService = WorkerLocalPersistentFolderServiceGrpc.newStub(inProcessChannelBuilder.build());
        persistentFolderService.getUserPersistentFolderUsage(request, responseObserver);
    }

    @Async
    public void asyncCreateUserPersistentFolder(WorkerLocalPersistentFolderDeleteParams request, StreamObserver<PersistentFolderDeletionResponse> responseObserver) {
        WorkerLocalPersistentFolderServiceGrpc.WorkerLocalPersistentFolderServiceStub persistentFolderService = WorkerLocalPersistentFolderServiceGrpc.newStub(inProcessChannelBuilder.build());
        persistentFolderService.deleteUserPersistentFolder(request, responseObserver);
    }
}
