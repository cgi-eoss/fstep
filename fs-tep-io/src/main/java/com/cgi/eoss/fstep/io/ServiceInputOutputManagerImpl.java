package com.cgi.eoss.fstep.io;

import com.cgi.eoss.fstep.io.download.DownloaderFacade;
import com.cgi.eoss.fstep.rpc.FstepServerClient;
import com.cgi.eoss.fstep.rpc.catalogue.CatalogueServiceGrpc;
import com.cgi.eoss.fstep.rpc.catalogue.FstepFileUri;
import com.cgi.eoss.fstep.rpc.catalogue.UriDataSourcePolicies;
import com.cgi.eoss.fstep.rpc.catalogue.UriDataSourcePolicy;
import com.cgi.eoss.fstep.rpc.catalogue.Uris;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.io.MoreFiles;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
public class ServiceInputOutputManagerImpl implements ServiceInputOutputManager {

    private static final String FSTEP_SERVICE_CONTEXT = "fstep://serviceContext/${serviceName}";

    private final FstepServerClient fstepServerClient;
    private final DownloaderFacade downloaderFacade;

    public ServiceInputOutputManagerImpl(FstepServerClient fstepServerClient, DownloaderFacade downloaderFacade) {
        this.fstepServerClient = fstepServerClient;
        this.downloaderFacade = downloaderFacade;
    }

    @Override
    public void prepareInput(Path target, Collection<URI> uris) throws IOException {
        if (uris.size() == 1) {
            // Single URI, download directly to the targetDir
            downloaderFacade.download(Iterables.getOnlyElement(uris), target);
        } else {
            // Multiple URIs, download to a subdir named after the filename portion of the URI
            Files.createDirectories(target);
            Map<URI, Optional<Path>> inputs = uris.stream().collect(Collectors.toMap(
                    uri -> uri,
                    uri -> Optional.of(target.resolve(MoreFiles.getNameWithoutExtension(Paths.get(uri.getPath()))))));
            downloaderFacade.download(inputs);
        }
    }

    @Override
    public Path getServiceContext(String serviceName) {
        try {
            URI uri = URI.create(StrSubstitutor.replace(FSTEP_SERVICE_CONTEXT, ImmutableMap.of("serviceName", serviceName)));
            return downloaderFacade.download(uri, null);
        } catch (Exception e) {
            throw new ServiceIoException("Could not construct service context for " + serviceName, e);
        }
    }

    @Override
    public boolean isSupportedProtocol(String scheme) {
        return downloaderFacade.isSupportedProtocol(scheme);
    }

    @Override
    public void cleanUp(Set<URI> unusedUris) {
        if (!unusedUris.isEmpty()) {
            CatalogueServiceGrpc.CatalogueServiceBlockingStub catalogueService = fstepServerClient.catalogueServiceBlockingStub();

            Uris.Builder fstepFileUris = Uris.newBuilder();
            unusedUris.forEach(uri -> fstepFileUris.addFileUris(FstepFileUri.newBuilder().setUri(uri.toString()).build()));

            UriDataSourcePolicies dataSourcePolicies = catalogueService.getDataSourcePolicies(fstepFileUris.build());
            dataSourcePolicies.getPoliciesList().stream()
                    .filter(uriPolicy -> uriPolicy.getPolicy() == UriDataSourcePolicy.Policy.REMOTE_ONLY)
                    .peek(uriPolicy -> LOG.info("Evicting REMOTE_ONLY data from: {}", uriPolicy))
                    .forEach(uriPolicy -> downloaderFacade.cleanUp(URI.create(uriPolicy.getUri().getUri())));
        }
    }

}
