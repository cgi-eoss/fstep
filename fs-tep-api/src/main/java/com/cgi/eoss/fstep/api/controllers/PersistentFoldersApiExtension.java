package com.cgi.eoss.fstep.api.controllers;
import java.util.concurrent.CountDownLatch;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cgi.eoss.fstep.model.PersistentFolder;
import com.cgi.eoss.fstep.model.PersistentFolder.Status;
import com.cgi.eoss.fstep.persistence.service.PersistentFolderDataService;
import com.cgi.eoss.fstep.rpc.LocalWorkerLocalPersistentFolderService;
import com.cgi.eoss.fstep.rpc.PersistentFolderCreationResponse;
import com.cgi.eoss.fstep.rpc.PersistentFolderParams;
import com.cgi.eoss.fstep.rpc.PersistentFolderSetQuotaResponse;
import com.cgi.eoss.fstep.rpc.WorkerLocalPersistentFolderParams;

import io.grpc.stub.StreamObserver;
import lombok.extern.log4j.Log4j2;

@RestController
@BasePathAwareController
@RequestMapping("/persistentfolders")
@Log4j2
public class PersistentFoldersApiExtension {

	LocalWorkerLocalPersistentFolderService localWorkerLocalPersistentFolderService;
	PersistentFolderDataService persistentFolderDataService;
	
	@Autowired
	public PersistentFoldersApiExtension(LocalWorkerLocalPersistentFolderService localWorkerLocalPersistentFolderService, PersistentFolderDataService persistentFolderDataService) {
		this.localWorkerLocalPersistentFolderService = localWorkerLocalPersistentFolderService;
		this.persistentFolderDataService = persistentFolderDataService;
	}
	
	@PostMapping("/{persistentFolderId}/activate")
	@PreAuthorize("hasAnyRole('ADMIN')")
    public void createPersistentFolderForUser(@ModelAttribute("persistentFolderId") PersistentFolder persistentFolder, @RequestParam(name = "quota") String quota) throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);
        PersistentFolderCreateObserver<PersistentFolderCreationResponse> observer = new PersistentFolderCreateObserver<>(latch);
        String folderPath = persistentFolder.getURI().getPath();
        String workerId = persistentFolder.getURI().getHost();
        LOG.info("Creating persistent folder on worker {} with path{} and quota {}", workerId, folderPath, quota);
        localWorkerLocalPersistentFolderService.asyncCreateUserPersistentFolder(WorkerLocalPersistentFolderParams.newBuilder().setWorkerId(workerId)
        		.setPersistentFolderParams(PersistentFolderParams.newBuilder().setPath(folderPath).setQuota(quota).build()).build(), observer);
        latch.await();
        persistentFolder.setStatus(Status.ACTIVE);
        persistentFolderDataService.save(persistentFolder);
        
	}
	
	@PostMapping("/{persistentFolderId}/quota")
	@PreAuthorize("hasAnyRole('ADMIN')")
    public void setPersistentFolderQuota(@ModelAttribute("persistentFolderId") PersistentFolder persistentFolder, @RequestParam(name = "quota") String quota) throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);
        PersistentFolderCreateObserver<PersistentFolderSetQuotaResponse> observer = new PersistentFolderCreateObserver<>(latch);
        String folderPath = persistentFolder.getURI().getPath();
        String workerId = persistentFolder.getURI().getHost();
        LOG.info("Creating persistent folder on worker {} with path{} and quota {}", workerId, folderPath, quota);
        localWorkerLocalPersistentFolderService.asyncSetUserPersistentFolderQuota(WorkerLocalPersistentFolderParams.newBuilder().setWorkerId(workerId)
        		.setPersistentFolderParams(PersistentFolderParams.newBuilder().setPath(folderPath).setQuota(quota).build()).build(), observer);
        latch.await();
        return;
 	}
	
	private static final class PersistentFolderCreateObserver<T> implements StreamObserver<T> {

        private final CountDownLatch latch;
        
        PersistentFolderCreateObserver(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void onNext(T value) {
            latch.countDown();
        }

        @Override
        public void onError(Throwable t) {
        	LOG.error("Failed to create persistent folder via REST API", t);
            latch.countDown();
        }

        @Override
        public void onCompleted() {
            // No-op, the user has long stopped listening here
        }
    }

}
