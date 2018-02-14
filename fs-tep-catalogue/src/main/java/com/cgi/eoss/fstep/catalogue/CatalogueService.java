package com.cgi.eoss.fstep.catalogue;

import com.cgi.eoss.fstep.model.Collection;
import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.model.internal.OutputFileMetadata;
import com.cgi.eoss.fstep.model.internal.OutputProductMetadata;
import com.cgi.eoss.fstep.model.internal.ReferenceDataMetadata;
import okhttp3.HttpUrl;
import org.geojson.GeoJsonObject;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

/**
 * <p>Centralised access to the FS-TEP catalogues of reference data, output products, and external product
 * references.</p>
 */
public interface CatalogueService {
    /**
     * <p>Create a new reference data file. The storage mechanism and implementation detail depends on the {@link
     * com.cgi.eoss.fstep.catalogue.files.ReferenceDataService} in use.</p>
     * <p>This will return a persisted entity.</p>
     *
     * @param referenceData
     * @param file
     * @return
     * @throws IOException
     */
    FstepFile ingestReferenceData(ReferenceDataMetadata referenceData, MultipartFile file) throws IOException;

    /**
     * <p>Create a path reference suitable for creating a new output product file.</p>
     * <p>This may be used as a "thin provisioning" method, e.g. to gain access to an output stream to write new output
     * file contents.</p>
     *
     * @param outputProduct
     * @param filename
     * @return
     */
    Path provisionNewOutputProduct(OutputProductMetadata outputProduct, String filename) throws IOException;

    /**
     * <p>Process an already-existing file, to be treated as an {@link FstepFile.Type#OUTPUT_PRODUCT}, to be ingested in the specified collection</p>
     * <p>This will return a persisted entity.</p>
     *
     * @param outputProduct
     * @param path
     * @return
     * @throws IOException
     */
    FstepFile ingestOutputProduct(OutputFileMetadata outputFileMetadata, Path path) throws IOException;
    
    /**
     * <p>Returns the identifier of the default output collection</p>
     * 
     */
    String getDefaultOutputProductCollection();
    
    /**
     * <p>Store an external product's metadata for later reference by FS-TEP.</p>
     *
     * @param geoJson
     * @return
     */
    FstepFile indexExternalProduct(GeoJsonObject geoJson);

    /**
     * <p>Resolve the given {@link FstepFile} into an appropriate Spring Resource descriptor.</p>
     *
     * @param file
     * @return
     */
    Resource getAsResource(FstepFile file);

    /**
     * <p>Remove the given FstepFile from all associated external catalogues, and finally the FS-TEP database itself.</p>
     *
     * @param file
     */
    void delete(FstepFile file) throws IOException;

    /**
     * <p>Generate an appropriate WMS URL for the given file.</p>
     *
     * @param type
     * @param uri
     * @return
     */
    HttpUrl getWmsUrl(FstepFile.Type type, URI uri);

    /**
     * <p>Determine whether the given user has read access to the object represented by the given URI ({@link
     * com.cgi.eoss.fstep.model.Databasket} or {@link FstepFile}).</p>
     * <p>This operation is recursive; access to a Databasket is granted only if all contents of the Databasket are also
     * readable.</p>
     *
     * @param user
     * @param uri
     * @return
     */
    boolean canUserRead(User user, URI uri);

    /**
     * <p>Creates the underlying collection represented by the collection parameter
     *
     * @param collection
     * @return
     */
    public boolean createOutputCollection(Collection collection);
    
    /**
     * <p>Deletes the underlying collection represented by the collection parameter
     *
     * @param collection
     * @return 
     * 
     */
    public boolean deleteOutputCollection(Collection collection);

    /**
     * <p>Check that the user has write access on the collection
     *
     * @param user
     * @param collectionIdentifier
     */
    boolean canUserWrite(User user, String collectionIdentifier);
}
