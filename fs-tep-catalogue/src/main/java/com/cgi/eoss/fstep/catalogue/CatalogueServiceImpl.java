package com.cgi.eoss.fstep.catalogue;

import com.cgi.eoss.fstep.catalogue.external.ExternalProductDataService;
import com.cgi.eoss.fstep.catalogue.files.OutputProductService;
import com.cgi.eoss.fstep.catalogue.files.ReferenceDataService;
import com.cgi.eoss.fstep.logging.Logging;
import com.cgi.eoss.fstep.model.Collection;
import com.cgi.eoss.fstep.model.DataSource;
import com.cgi.eoss.fstep.model.Databasket;
import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.model.internal.OutputFileMetadata;
import com.cgi.eoss.fstep.model.internal.OutputProductMetadata;
import com.cgi.eoss.fstep.model.internal.ReferenceDataMetadata;
import com.cgi.eoss.fstep.persistence.service.CollectionDataService;
import com.cgi.eoss.fstep.persistence.service.DataSourceDataService;
import com.cgi.eoss.fstep.persistence.service.DatabasketDataService;
import com.cgi.eoss.fstep.persistence.service.FstepFileDataService;
import com.cgi.eoss.fstep.persistence.service.UserDataService;
import com.cgi.eoss.fstep.rpc.FileStream;
import com.cgi.eoss.fstep.rpc.FileStreamIOException;
import com.cgi.eoss.fstep.rpc.FileStreamServer;
import com.cgi.eoss.fstep.rpc.catalogue.CatalogueServiceGrpc;
import com.cgi.eoss.fstep.rpc.catalogue.DatabasketContents;
import com.cgi.eoss.fstep.rpc.catalogue.FstepFileUri;
import com.cgi.eoss.fstep.rpc.catalogue.UriDataSourcePolicies;
import com.cgi.eoss.fstep.rpc.catalogue.UriDataSourcePolicy;
import com.cgi.eoss.fstep.rpc.catalogue.Uris;
import com.cgi.eoss.fstep.security.FstepPermission;
import com.cgi.eoss.fstep.security.FstepSecurityService;
import com.google.common.base.Stopwatch;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import org.apache.logging.log4j.CloseableThreadContext;
import org.geojson.GeoJsonObject;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@GRpcService
@Log4j2
public class CatalogueServiceImpl extends CatalogueServiceGrpc.CatalogueServiceImplBase implements CatalogueService {
    
    private final FstepFileDataService fstepFileDataService;
    private final CollectionDataService collectionDataService;
    private final DataSourceDataService dataSourceDataService;
    private final DatabasketDataService databasketDataService;
    private final OutputProductService outputProductService;
    private final ReferenceDataService referenceDataService;
    private final ExternalProductDataService externalProductDataService;
    private final FstepSecurityService securityService;
    private final UserDataService userDataService;
    
    @Autowired
    public CatalogueServiceImpl(FstepFileDataService fstepFileDataService, CollectionDataService collectionDataService, DataSourceDataService dataSourceDataService, DatabasketDataService databasketDataService, OutputProductService outputProductService, ReferenceDataService referenceDataService, ExternalProductDataService externalProductDataService, FstepSecurityService securityService, UserDataService userDataService) {
        this.fstepFileDataService = fstepFileDataService;
        this.collectionDataService = collectionDataService;
        this.dataSourceDataService = dataSourceDataService;
        this.databasketDataService = databasketDataService;
        this.outputProductService = outputProductService;
        this.referenceDataService = referenceDataService;
        this.externalProductDataService = externalProductDataService;
        this.securityService = securityService;
        this.userDataService = userDataService;
    }

    @Override
    public FstepFile ingestReferenceData(ReferenceDataMetadata referenceData, MultipartFile file) throws IOException {
        FstepFile fstepFile = referenceDataService.ingest(referenceData.getOwner(), referenceData.getFilename(), referenceData.getFiletype(), referenceData.getUserProperties(), file);
        fstepFile.setDataSource(dataSourceDataService.getForRefData(fstepFile));
        return fstepFileDataService.save(fstepFile);
    }

    @Override
    public Path provisionNewOutputProduct(OutputProductMetadata outputProduct, String filename) throws IOException {
        return outputProductService.provision(outputProduct.getJobId(), filename);
    }
    
    @Override
    public String getDefaultOutputProductCollection() {
        return outputProductService.getDefaultCollection();
    }
    
    @Override
    public FstepFile ingestOutputProduct(OutputFileMetadata outputFileMetadata, Path path) throws IOException {
        OutputProductMetadata outputProductMetadata = outputFileMetadata.getOutputProductMetadata();
        String collection = (String) outputProductMetadata.getProductProperties().get("collection");
        if (collection == null) {
            collection = getDefaultOutputProductCollection();
        }
        ensureOutputCollectionExists(collection);
        FstepFile fstepFile = outputProductService.ingest(
                collection,
                outputProductMetadata.getOwner(),
                outputProductMetadata.getJobId(),
                outputFileMetadata.getCrs(),
                outputFileMetadata.getGeometry(),
                outputFileMetadata.getStartDateTime(),
                outputFileMetadata.getEndDateTime(),
                outputProductMetadata.getProductProperties(),
                path);
        fstepFile.setDataSource(dataSourceDataService.getForService(outputProductMetadata.getService()));
        fstepFile.setCollection(collectionDataService.getByIdentifier(collection));
        return fstepFileDataService.save(fstepFile);
    }

    private void ensureOutputCollectionExists(String collectionIdentifier) {
        Collection collection = collectionDataService.getByIdentifier(collectionIdentifier);
        if (collection == null) {
            createOutputCollection(collectionIdentifier);
        }
    }

    private void createOutputCollection(String collectionIdentifier) {
       if (collectionIdentifier.equals(getDefaultOutputProductCollection())) {
           Collection collection = new Collection(getDefaultOutputProductCollection(), userDataService.getDefaultUser());
           collection.setDescription("Output Products");
           collection.setProductsType("Misc");
           collection.setIdentifier(getDefaultOutputProductCollection());
           collectionDataService.save(collection);
           securityService.publish(Collection.class, collection.getId());
       }
        
    }

    @Override
    public FstepFile indexExternalProduct(GeoJsonObject geoJson) {
        // This will return an already-persistent object
        FstepFile fstepFile = externalProductDataService.ingest(geoJson);
        fstepFile.setDataSource(dataSourceDataService.getForExternalProduct(fstepFile));
        return fstepFile;
    }

    @Override
    public Resource getAsResource(FstepFile file) {
        switch (file.getType()) {
            case REFERENCE_DATA:
                return referenceDataService.resolve(file);
            case OUTPUT_PRODUCT:
                return outputProductService.resolve(file);
            case EXTERNAL_PRODUCT:
                return externalProductDataService.resolve(file);
            default:
                throw new UnsupportedOperationException("Unable to materialise FstepFile: " + file);
        }
    }

    @Override
    public void delete(FstepFile file) throws IOException {
        switch (file.getType()) {
            case REFERENCE_DATA:
                referenceDataService.delete(file);
                break;
            case OUTPUT_PRODUCT:
                outputProductService.delete(file);
                break;
            case EXTERNAL_PRODUCT:
                externalProductDataService.delete(file);
                break;
        }
        fstepFileDataService.delete(file);
    }

    @Override
    public HttpUrl getWmsUrl(FstepFile.Type type, URI uri) {
        switch (type) {
            case OUTPUT_PRODUCT:
                // TODO Use the CatalogueUri pattern to determine file attributes
                String[] pathComponents = uri.getPath().split("/");
                String jobId = pathComponents[1];
                String filename = pathComponents[pathComponents.length-1];
                return outputProductService.getWmsUrl(jobId, filename);
            default:
                return null;
        }
    }

    @Override
    public boolean canUserRead(User user, URI uri) {
        if (uri.getScheme().equals("fstep") && uri.getHost().equals("databasket")) {
            Databasket databasket = getDatabasketFromUri(uri.toASCIIString());

            if (!securityService.hasUserPermission(user, FstepPermission.READ, Databasket.class, databasket.getId())) {
                logAccessFailure(uri);
                return false;
            }

            return databasket.getFiles().stream().allMatch(fstepFile -> canUserRead(user, fstepFile.getUri()));
        } else {
            FstepFile fstepFile = fstepFileDataService.getByUri(uri);

            if (fstepFile != null) {
                if (!securityService.hasUserPermission(user, FstepPermission.READ, FstepFile.class, fstepFile.getId())) {
                    logAccessFailure(fstepFile.getUri());
                    return false;
                }
            }

            return true;
        }
    }
    
    @Override
    public boolean canUserWrite(User user, String collectionIdentifier) {
        Collection collection = collectionDataService.getByIdentifier(collectionIdentifier);
        return securityService.hasUserPermission(user, FstepPermission.WRITE, Collection.class, collection.getId());
    }

    private void logAccessFailure(URI uri) {
        try (CloseableThreadContext.Instance ctc = Logging.userLoggingContext()) {
            LOG.info("Access denied to FS-TEP resource: {}", uri);
        }
    }

    @Override
    public void downloadFstepFile(FstepFileUri request, StreamObserver<FileStream> responseObserver) {
        FstepFile file = fstepFileDataService.getByUri(request.getUri());
        Resource fileResource = getAsResource(file);

        try (FileStreamServer fileStreamServer = new FileStreamServer(null, responseObserver) {
            @Override
            protected FileStream.FileMeta buildFileMeta() {
                try {
                    return FileStream.FileMeta.newBuilder()
                            .setFilename(fileResource.getFilename())
                            .setSize(fileResource.contentLength())
                            .build();
                } catch (IOException e) {
                    throw new FileStreamIOException(e);
                }
            }

            @Override
            protected ReadableByteChannel buildByteChannel() {
                try {
                    return Channels.newChannel(fileResource.getInputStream());
                } catch (IOException e) {
                    throw new FileStreamIOException(e);
                }
            }
        }) {
            Stopwatch stopwatch = Stopwatch.createStarted();
            fileStreamServer.streamFile();
            LOG.info("Transferred FstepFile {} ({} bytes) in {}", fileResource.getFilename(), fileResource.contentLength(), stopwatch.stop().elapsed());
        } catch (IOException e) {
            LOG.error("Failed to serve file download for {}", request.getUri(), e);
            responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
        } catch (InterruptedException e) {
            // Restore interrupted state
            Thread.currentThread().interrupt();
            LOG.error("Failed to serve file download for {}", request.getUri(), e);
            responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
        }

    }

    @Override
    public void getDatabasketContents(com.cgi.eoss.fstep.rpc.catalogue.Databasket request, StreamObserver<DatabasketContents> responseObserver) {
        try {
            // TODO Extract databasket ID from CatalogueUri pattern
            Databasket databasket = getDatabasketFromUri(request.getUri());

            DatabasketContents.Builder responseBuilder = DatabasketContents.newBuilder();
            databasket.getFiles().forEach(f -> responseBuilder.addFiles(
                    com.cgi.eoss.fstep.rpc.catalogue.FstepFile.newBuilder()
                            .setFilename(f.getFilename())
                            .setUri(FstepFileUri.newBuilder().setUri(f.getUri().toASCIIString()).build())
                            .build()
                    )
            );

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOG.error("Failed to list databasket contents for {}", request.getUri(), e);
            responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
        }
    }

    @Override
    public void getDataSourcePolicies(Uris request, StreamObserver<UriDataSourcePolicies> responseObserver) {
        try {
            UriDataSourcePolicies.Builder responseBuilder = UriDataSourcePolicies.newBuilder();

            for (FstepFileUri fileUri : request.getFileUrisList()) {
                FstepFile fstepFile = fstepFileDataService.getByUri(fileUri.getUri());
                DataSource dataSource;

                if (fstepFile != null) {
                    dataSource = fstepFile.getDataSource() != null ? fstepFile.getDataSource() :
                            dataSourceDataService.getByName(URI.create(fileUri.getUri()).getScheme());
                } else {
                    dataSource = dataSourceDataService.getByName(URI.create(fileUri.getUri()).getScheme());
                }

                LOG.debug("Inferred DataSource {} from FstepFile: {}", dataSource, fileUri.getUri());

                // Default to CACHE mode
                DataSource.Policy policy = dataSource != null ? dataSource.getPolicy() : DataSource.Policy.CACHE;

                responseBuilder.addPolicies(UriDataSourcePolicy.newBuilder()
                        .setUri(fileUri)
                        .setPolicy(UriDataSourcePolicy.Policy.valueOf(policy.toString()))
                        .build());
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOG.error("Failed to list datasource access policies contents for {}", request.getFileUrisList(), e);
            responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
        }
    }

    private Databasket getDatabasketFromUri(String uri) {
        Matcher uriIdMatcher = Pattern.compile(".*/([0-9]+)$").matcher(uri);
        if (!uriIdMatcher.matches()) {
            throw new CatalogueException("Failed to load databasket for URI: " + uri);
        }
        Long databasketId = Long.parseLong(uriIdMatcher.group(1));
        Databasket databasket = Optional.ofNullable(databasketDataService.getById(databasketId)).orElseThrow(() -> new CatalogueException("Failed to load databasket for ID " + databasketId));
        LOG.debug("Listing databasket contents for id {}", databasketId);
        return databasket;
    }

    @Override
    public boolean createOutputCollection(Collection collection) {
        return outputProductService.createCollection(collection); 
    }

    @Override
    public boolean deleteOutputCollection(Collection collection) {
        return outputProductService.deleteCollection(collection);
    }

}
