package com.cgi.eoss.fstep.quotas;

import java.net.URI;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cgi.eoss.fstep.model.PersistentFolder;
import com.cgi.eoss.fstep.model.UsageType;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.persistence.service.FstepFileDataService;
import com.cgi.eoss.fstep.persistence.service.PersistentFolderDataService;
import com.cgi.eoss.fstep.model.PersistentFolder.Status;
import com.cgi.eoss.fstep.rpc.LocalWorkerLocalPersistentFolderService;
import com.cgi.eoss.fstep.rpc.PersistentFolderUsageParams;
import com.cgi.eoss.fstep.rpc.PersistentFolderUsageResponse;
import com.cgi.eoss.fstep.rpc.WorkerLocalPersistentFolderUsageParams;

import io.grpc.stub.StreamObserver;

@Component
public class UsageServiceImpl implements UsageService{

	private PersistentFolderDataService persistentFolderDataService;
	private LocalWorkerLocalPersistentFolderService localWorkerLocalPersistentFolderService;
	private FstepFileDataService fstepFileDataService;
	
	@Autowired
	public UsageServiceImpl(PersistentFolderDataService persistentFolderDataService, LocalWorkerLocalPersistentFolderService localWorkerLocalPersistentFolderService, FstepFileDataService fstepFileDataService) {
		this.persistentFolderDataService = persistentFolderDataService;
		this.localWorkerLocalPersistentFolderService = localWorkerLocalPersistentFolderService;
		this.fstepFileDataService = fstepFileDataService;
	}
	
	@Override
	public
	Optional<Long> getUsage(User user, UsageType usageType) {
		switch (usageType) {
			case PERSISTENT_STORAGE_MB: return Optional.ofNullable(getPersistentFolderUsage(user));
			case FILES_STORAGE_MB: return Optional.of(fstepFileDataService.sumFilesizeByOwner(user)/ 1_048_576);
			default: return Optional.empty();
		}
		
	}
	
	Long getPersistentFolderUsage(User user) {
		final CountDownLatch latch = new CountDownLatch(1);
    	PersistentFolderUsageObserver observer = new PersistentFolderUsageObserver(latch);
        Optional<PersistentFolder> persistentFolder = persistentFolderDataService.findByOwnerAndStatus(user, Status.ACTIVE).stream().findFirst();
        if (persistentFolder.isPresent()) {
        	URI locator = persistentFolder.get().getLocator();
            String workerId = locator.getHost();
            String path = locator.getPath();
	        localWorkerLocalPersistentFolderService.asyncGetUserPersistentFolderUsage(WorkerLocalPersistentFolderUsageParams.newBuilder().setWorkerId(workerId)
	        		.setPersistentFolderUsageParams(PersistentFolderUsageParams.newBuilder().setPath(path).build()).build(), observer);
	        try {
				latch.await();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
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
            // No-op
        }
    }

}