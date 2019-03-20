package com.cgi.eoss.fstep.api.controllers;

import java.net.URI;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cgi.eoss.fstep.model.PersistentFolder;
import com.cgi.eoss.fstep.model.PersistentFolder.Status;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.persistence.service.FstepFileDataService;
import com.cgi.eoss.fstep.persistence.service.PersistentFolderDataService;
import com.cgi.eoss.fstep.rpc.LocalWorkerLocalPersistentFolderService;
import com.cgi.eoss.fstep.rpc.PersistentFolderUsageParams;
import com.cgi.eoss.fstep.rpc.PersistentFolderUsageResponse;
import com.cgi.eoss.fstep.rpc.WorkerLocalPersistentFolderUsageParams;
import com.cgi.eoss.fstep.security.FstepSecurityService;

import io.grpc.stub.StreamObserver;

/**
 * <p>Functionality for platform usage metrics retrieval </p>
 */
@RestController
@BasePathAwareController
@RequestMapping("/usage")
public class UsageApi {

	private final FstepSecurityService fstepSecurityService;
    private final FstepFileDataService fstepFileDataService;
    private final LocalWorkerLocalPersistentFolderService localWorkerLocalPersistentFolderService;
    private final PersistentFolderDataService persistentFolderDataService;
    
    @Autowired
    public UsageApi(FstepSecurityService fstepSecurityService, FstepFileDataService fstepFileDataService, LocalWorkerLocalPersistentFolderService localWorkerLocalPersistentFolderService, PersistentFolderDataService persistentFolderDataService) {
    	this.fstepSecurityService = fstepSecurityService;
    	this.fstepFileDataService = fstepFileDataService;
    	this.persistentFolderDataService = persistentFolderDataService;
    	this.localWorkerLocalPersistentFolderService = localWorkerLocalPersistentFolderService;
    }
    
    /**
     * Get current usage of storage for FstepFiles
     * @return File size in MB
     */
    @GetMapping("/files/storage")
    public Long filesStorage() {
    	User user = fstepSecurityService.getCurrentUser();
    	return fstepFileDataService.sumFilesizeByOwner(user)/ 1_048_576;
    	
    }
    
    /**
     * Get current usage of storage for the persistent folder
     * @return File size in MB
     * @throws InterruptedException 
     */
    @GetMapping("/persistentFolder/storage")
    public Long persistentFolderStorage() throws InterruptedException {
    	User user = fstepSecurityService.getCurrentUser();
    	final CountDownLatch latch = new CountDownLatch(1);
    	PersistentFolderUsageObserver observer = new PersistentFolderUsageObserver(latch);
        Optional<PersistentFolder> persistentFolder = persistentFolderDataService.findByOwnerAndStatus(user, Status.ACTIVE).stream().findFirst();
        if (persistentFolder.isPresent()) {
        	URI locator = persistentFolder.get().getLocator();
            String workerId = locator.getHost();
            String path = locator.getPath();
	        localWorkerLocalPersistentFolderService.asyncGetUserPersistentFolderUsage(WorkerLocalPersistentFolderUsageParams.newBuilder().setWorkerId(workerId)
	        		.setPersistentFolderUsageParams(PersistentFolderUsageParams.newBuilder().setPath(path).build()).build(), observer);
	        latch.await();
	        Long usage = 0L;
	        if (observer.getUsage() != null) {
	        	usage = Long.parseLong(observer.getUsage());
	        }
	        return usage/ 1_048_576;
        }
        return 0L;
        
	}
	
	private static final class PersistentFolderUsageObserver implements StreamObserver<PersistentFolderUsageResponse> {

        private final CountDownLatch latch;
        private String usage;
        
        PersistentFolderUsageObserver(CountDownLatch latch) {
            this.latch = latch;
        }

        public String getUsage() {
			return usage;
		}

		@Override
        public void onNext(PersistentFolderUsageResponse value) {
			usage = value.getUsage();
            latch.countDown();
        }

        @Override
        public void onError(Throwable t) {
            latch.countDown();
        }

        @Override
        public void onCompleted() {
            // No-op, the user has long stopped listening here
        }
    }
}
