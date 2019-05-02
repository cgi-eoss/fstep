package com.cgi.eoss.fstep.io;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;

import lombok.Data;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class LocalXfsPersistentFolderProvider implements PersistentFolderProvider {

	@Data
	public class ProcessResult {
		private final int returnCode;
		
		private final List<String> output;
	}

	private Path basePath;
	
	public LocalXfsPersistentFolderProvider(String basePath) {
		this(Paths.get(basePath));
	}
	
	public LocalXfsPersistentFolderProvider(Path basePath) {
		this.basePath = basePath;
	}
	

	private Path constructFullPath(String path) {
		Path folderPath = Paths.get(path);
		if (folderPath.isAbsolute()) {
			folderPath = Paths.get("/").relativize(folderPath);
		}
		return basePath.resolve(folderPath);
	}

	@Override
	public void createUserFolderWithQuota(String path, String quota) throws IOException {
		LOG.info("Creating folder {}", constructFullPath(path).toString());
		Path userFolder = constructFullPath(path);
		LOG.info("Creating folder {}", userFolder.toString());
		File userFolderDirectory = userFolder.toFile();
		userFolderDirectory.mkdirs();
		//TODO Project id must be calculated...
		int projectId = Integer.parseInt(Paths.get(path).getParent().getName(0).toString());
		ProcessResult createProjectResult = executeShellCommand("sudo", "xfs_quota", "-x", "-c", "'project -s -p " + userFolder.toString() + " " + projectId + "'", basePath.toString());
		if (createProjectResult.returnCode != 0){
			throw new IOException();
		}
		
		setQuota(path, quota);
		
	}

	@Override
	public void setQuota(String path, String quota) throws IOException {
		int projectId = Integer.parseInt(Paths.get(path).getParent().getName(0).toString());
		ProcessResult setQuotaResult = executeShellCommand("sudo", "xfs_quota", "-x", "-c", "'limit -p bhard=" + quota + " " + projectId + "'", basePath.toString());
		if (setQuotaResult.returnCode != 0){
			throw new IOException();
		}
	}

	@Override
	public boolean userFolderExists(String path) throws IOException {
		LOG.info("Checking existence of folder {}", constructFullPath(path).toString());
		return Files.exists(constructFullPath(path));
	}
	
	public long getUsageInBytes(String path) throws IOException{
		Path userFolder = constructFullPath(path);
		return Files.walk(userFolder)
	      .filter(p -> p.toFile().isFile())
	      .mapToLong(p -> p.toFile().length())
	      .sum();
	}
	
	private ProcessResult executeShellCommand(String... args) throws IOException {
		LOG.debug("Executing shell command  {}", Arrays.stream(args).collect(Collectors.joining(" ")));
		DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
		ExecuteWatchdog watchdog  = new ExecuteWatchdog(60000);
		Executor exec = new DefaultExecutor();
		CommandLine commandLine = new CommandLine(args[0]);
		for (int i = 1; i < args.length; i++) {
			commandLine.addArgument(args[i]);
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PumpStreamHandler psh = new PumpStreamHandler( out );

		exec.setStreamHandler( psh );
		exec.setWatchdog( watchdog );

		exec.execute(commandLine, resultHandler );
		try {
			resultHandler.waitFor();
		} catch (InterruptedException e) {
			throw new IOException();
		}
		if (!resultHandler.hasResult()) {
			throw new IOException(resultHandler.getException());
		}
		
		return new ProcessResult(resultHandler.getExitValue(), Arrays.asList(new String(out.toByteArray()).split("\\n")));
	}
	
	@Override
	public void deleteUserFolder(String path) throws IOException {
		Path userFolder = constructFullPath(path);
		FileUtils.deleteDirectory(userFolder.toFile());
	}
}
