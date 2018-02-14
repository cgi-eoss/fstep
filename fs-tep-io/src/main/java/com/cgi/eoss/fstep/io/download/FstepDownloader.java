package com.cgi.eoss.fstep.io.download;

import com.cgi.eoss.fstep.rpc.FileStream;
import com.cgi.eoss.fstep.rpc.FileStreamClient;
import com.cgi.eoss.fstep.rpc.FstepServerClient;
import com.cgi.eoss.fstep.rpc.GetServiceContextFilesParams;
import com.cgi.eoss.fstep.rpc.ServiceContextFiles;
import com.cgi.eoss.fstep.rpc.ShortFile;
import com.cgi.eoss.fstep.rpc.catalogue.CatalogueServiceGrpc;
import com.cgi.eoss.fstep.rpc.catalogue.Databasket;
import com.cgi.eoss.fstep.rpc.catalogue.DatabasketContents;
import com.cgi.eoss.fstep.rpc.catalogue.FstepFile;
import com.cgi.eoss.fstep.rpc.catalogue.FstepFileUri;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import lombok.extern.log4j.Log4j2;
import org.jooq.lambda.Unchecked;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.nio.file.attribute.PosixFilePermission.GROUP_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.GROUP_READ;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;

@Component
@Log4j2
public class FstepDownloader implements Downloader {

    private static final EnumSet<PosixFilePermission> EXECUTABLE_PERMS = EnumSet.of(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE, GROUP_READ, GROUP_EXECUTE, OTHERS_READ, OTHERS_EXECUTE);
    private static final EnumSet<PosixFilePermission> NON_EXECUTABLE_PERMS = EnumSet.of(OWNER_READ, OWNER_WRITE, GROUP_READ, OTHERS_READ);

    private final FstepServerClient fstepServerClient;
    private final DownloaderFacade downloaderFacade;

    public FstepDownloader(FstepServerClient fstepServerClient, DownloaderFacade downloaderFacade) {
        this.fstepServerClient = fstepServerClient;
        this.downloaderFacade = downloaderFacade;
    }

    @PostConstruct
    public void postConstruct() {
        downloaderFacade.registerDownloader(this);
    }

    @PreDestroy
    public void preDestroy() {
        downloaderFacade.unregisterDownloader(this);
    }

    @Override
    public Set<String> getProtocols() {
        return ImmutableSet.of("fstep");
    }

    @Override
    public Path download(Path targetDir, URI uri) throws IOException {
        LOG.info("Downloading: {}", uri);

        switch (uri.getHost()) {
            case "serviceContext":
                return downloadServiceContextFiles(targetDir, Paths.get(uri.getPath()).getFileName().toString());
            case "outputProduct":
                return downloadFstepFile(targetDir, uri);
            case "refData":
                return downloadFstepFile(targetDir, uri);
            case "databasket":
                return downloadDatabasket(targetDir, uri);
            default:
                throw new UnsupportedOperationException("Unrecognised fstep:// URI type: " + uri.getHost());
        }
    }

    private Path downloadServiceContextFiles(Path targetDir, String serviceName) throws IOException {
        ServiceContextFiles serviceContextFiles = fstepServerClient.serviceContextFilesServiceBlockingStub()
                .getServiceContextFiles(GetServiceContextFilesParams.newBuilder().setServiceName(serviceName).build());

        for (ShortFile f : serviceContextFiles.getFilesList()) {
            Path targetFile = targetDir.resolve(f.getFilename());
            Set<PosixFilePermission> permissions = f.getExecutable() ? EXECUTABLE_PERMS : NON_EXECUTABLE_PERMS;

            LOG.debug("Writing service context file for {} to: {}", serviceName, targetFile);
            Files.write(targetFile, f.getContent().toByteArray(), CREATE, TRUNCATE_EXISTING);
            Files.setPosixFilePermissions(targetFile, permissions);
        }

        return targetDir;
    }

    private Path downloadFstepFile(Path targetDir, URI uri) throws IOException {
        FstepFileUri fstepFileUri = FstepFileUri.newBuilder().setUri(uri.toString()).build();

        CatalogueServiceGrpc.CatalogueServiceStub catalogueService = fstepServerClient.catalogueServiceStub();

        try (FileStreamClient<FstepFileUri> fileStreamClient = new FileStreamClient<FstepFileUri>() {
            @Override
            public OutputStream buildOutputStream(FileStream.FileMeta fileMeta) throws IOException {
                setOutputPath(targetDir.resolve(fileMeta.getFilename()));
                LOG.info("Transferring FstepFile ({} bytes) to {}", fileMeta.getSize(), getOutputPath());
                return new BufferedOutputStream(Files.newOutputStream(getOutputPath(), CREATE, TRUNCATE_EXISTING, WRITE));
            }
        }) {
            catalogueService.downloadFstepFile(fstepFileUri, fileStreamClient.getFileStreamObserver());
            fileStreamClient.getLatch().await();
            return fileStreamClient.getOutputPath();
        } catch (InterruptedException e) {
            // Restore interrupted state, then re-throw
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }

    private Path downloadDatabasket(Path targetDir, URI uri) {
        CatalogueServiceGrpc.CatalogueServiceBlockingStub catalogueService = fstepServerClient.catalogueServiceBlockingStub();

        DatabasketContents databasketContents = catalogueService.getDatabasketContents(Databasket.newBuilder().setUri(uri.toASCIIString()).build());

        databasketContents.getFilesList().forEach(Unchecked.consumer((FstepFile fstepFile) -> {
            URI fileUri = URI.create(fstepFile.getUri().getUri());
            Path cacheDir = downloaderFacade.download(fileUri, null);
            LOG.info("Successfully downloaded from databasket: {} ({})", cacheDir, fileUri);
            symlinkDatabasketContents(cacheDir, targetDir, fstepFile.getFilename());
        }));

        return targetDir;
    }

    private void symlinkDatabasketContents(Path cacheDir, Path targetDir, String filename) throws IOException {
        try (Stream<Path> cacheDirList = Files.list(cacheDir)) {
            Set<Path> cacheDirContents = cacheDirList
                    .filter(f -> Files.isRegularFile(f) && !f.getFileName().toString().equals(".uri"))
                    .collect(Collectors.toSet());

            if (cacheDirContents.size() == 1) {
                // If the cache directory contains only one file (excluding .uri) then symlink it directly
                Path file = Iterables.getOnlyElement(cacheDirContents);
                Files.createSymbolicLink(targetDir.resolve(file.getFileName()), file);
            } else {
                // Otherwise symlink the FstepFile filename to the cache directory
                Files.createSymbolicLink(targetDir.resolve(filename), cacheDir);
            }
        }
    }

}
