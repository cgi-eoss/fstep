package com.cgi.eoss.fstep.orchestrator.service;

import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cgi.eoss.fstep.rpc.PersistentFolderCreationResponse;
import com.cgi.eoss.fstep.rpc.PersistentFolderDeletionResponse;
import com.cgi.eoss.fstep.rpc.PersistentFolderSetQuotaResponse;
import com.cgi.eoss.fstep.rpc.PersistentFolderUsageResponse;
import com.cgi.eoss.fstep.rpc.WorkerLocalPersistentFolderDeleteParams;
import com.cgi.eoss.fstep.rpc.WorkerLocalPersistentFolderParams;
import com.cgi.eoss.fstep.rpc.WorkerLocalPersistentFolderServiceGrpc.WorkerLocalPersistentFolderServiceImplBase;
import com.cgi.eoss.fstep.rpc.WorkerLocalPersistentFolderUsageParams;
import com.cgi.eoss.fstep.rpc.worker.FstepWorkerGrpc.FstepWorkerBlockingStub;

import io.grpc.stub.StreamObserver;
import lombok.extern.log4j.Log4j2;


@Service
@GRpcService
@Log4j2
public class WorkerLocalPersistentFolderService extends WorkerLocalPersistentFolderServiceImplBase {

	private final CachingWorkerFactory workerFactory;
	
	@Autowired
	public WorkerLocalPersistentFolderService(CachingWorkerFactory workerFactory) {
		this.workerFactory = workerFactory;
	}

	@Override
	public void createUserPersistentFolder(WorkerLocalPersistentFolderParams request,
			StreamObserver<PersistentFolderCreationResponse> responseObserver) {
		String workerId = request.getWorkerId();
		LOG.info("Locating worker {}", workerId);
		FstepWorkerBlockingStub worker = workerFactory.getWorkerById(workerId);
		LOG.info("Sending request for persistent folder creation on worker {} with path{} and quota {}", workerId, request.getPersistentFolderParams().getPath(), request.getPersistentFolderParams().getQuota());
        try {
        	PersistentFolderCreationResponse response = worker.createUserPersistentFolder(request.getPersistentFolderParams());
        	responseObserver.onNext(response);
    		responseObserver.onCompleted();
    	}
        catch (Exception e) {
        	responseObserver.onError(e);
        }
	}
	
	@Override
	public void setUserPersistentFolderQuota(WorkerLocalPersistentFolderParams request,
			StreamObserver<PersistentFolderSetQuotaResponse> responseObserver) {
		FstepWorkerBlockingStub worker = workerFactory.getWorkerById(request.getWorkerId());
		try {
			PersistentFolderSetQuotaResponse response = worker.setUserPersistentFolderQuota(request.getPersistentFolderParams());
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (Exception e) {
			responseObserver.onError(e);
		}
	}

	@Override
	public void getUserPersistentFolderUsage(WorkerLocalPersistentFolderUsageParams request,
			StreamObserver<PersistentFolderUsageResponse> responseObserver) {
		FstepWorkerBlockingStub worker = workerFactory.getWorkerById(request.getWorkerId());
		try {
			PersistentFolderUsageResponse response = worker.getUserPersistentFolderUsage(request.getPersistentFolderUsageParams());
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (Exception e) {
			responseObserver.onError(e);
		}
	}

	@Override
	public void deleteUserPersistentFolder(WorkerLocalPersistentFolderDeleteParams request,
			StreamObserver<PersistentFolderDeletionResponse> responseObserver) {
		FstepWorkerBlockingStub worker = workerFactory.getWorkerById(request.getWorkerId());
		try {
			PersistentFolderDeletionResponse response = worker.deleteUserPersistentFolder(request.getPersistentFolderDeleteParams());
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (Exception e) {
			responseObserver.onError(e);
		}
	}

}
