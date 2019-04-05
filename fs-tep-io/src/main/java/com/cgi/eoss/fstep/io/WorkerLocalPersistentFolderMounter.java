package com.cgi.eoss.fstep.io;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class WorkerLocalPersistentFolderMounter implements PersistentFolderMounter{

	private String workerId;
	private Path basePath;
	
	public WorkerLocalPersistentFolderMounter(String workerId, Path basePath) {
		this.workerId = workerId;
		this.basePath = basePath;
	}
	
	public WorkerLocalPersistentFolderMounter(String workerId, String basePath) {
		this(workerId, Paths.get(basePath));
	}
	
	@Override
	public List<Path> bindUserPersistentFolder(URI folderUri, Path bindingDir) throws IOException {
		String path = folderUri.getPath();
		LOG.info("Binding folder {} to dir {}", path, bindingDir);
		Path userFolder = constructFullPath(path);
		if (!Files.exists(userFolder)) {
			throw new IOException();
		}
		
		Path symLink = bindingDir.resolve("files");
		if (Files.exists(symLink)){
			Files.delete(symLink);
		}
		Files.createSymbolicLink(bindingDir.resolve("files"), userFolder);
		return Collections.singletonList(userFolder);
	}

	@Override
	public boolean supportsPersistentFolder(URI uri) throws IOException {
		return uri.getScheme().equals("worker") && uri.getHost().equals(workerId);
	}
	
	private Path constructFullPath(String path) {
		Path folderPath = Paths.get(path);
		if (folderPath.isAbsolute()) {
			folderPath = Paths.get("/").relativize(folderPath);
		}
		return basePath.resolve(folderPath);
	}


}
