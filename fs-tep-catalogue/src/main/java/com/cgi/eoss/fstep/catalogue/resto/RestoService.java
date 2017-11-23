package com.cgi.eoss.fstep.catalogue.resto;

import com.cgi.eoss.fstep.model.Collection;
import com.cgi.eoss.fstep.model.FstepFile;
import java.util.UUID;
import org.geojson.GeoJsonObject;

/**
 * <p>Facade to a Resto instance, to enable FS-TEP OpenSearch Geo/Time functionality.</p>
 */
public interface RestoService {
    /**
     * <p>Ingest the given GeoJsonObject to the Resto catalogue, in the FS-TEP Reference Data collection, and return the
     * new record's UUID.</p>
     */
    UUID ingestReferenceData(GeoJsonObject object);
    
    /**
     * Ingest the given GeoJsonObject product in a specific collection
     */
    UUID ingestOutputProduct(String collection, GeoJsonObject object);

    /**
     * <p>Ingest the given GeoJsonObject to the Resto catalogue, in the given collection, and return the new record's
     * UUID.</p>
     */
    UUID ingestExternalProduct(String collection, GeoJsonObject object);

    /**
     * <p>Remove the given FS-TEP Output product from the Resto catalogue.</p>
     */
    void deleteOutputProduct(UUID restoId);
    
    /**
     * <p>Remove the given FS-TEP Reference Data product from the Resto catalogue.</p>
     */
    void deleteReferenceData(UUID restoId);

    /**
     * @return The Resto catalogue GeoJSON data for the given FstepFile.
     */
    GeoJsonObject getGeoJson(FstepFile fstepFile);

    /**
     * @return The Resto catalogue GeoJSON data for the given FstepFile, or null if any exception is encountered.
     */
    GeoJsonObject getGeoJsonSafe(FstepFile fstepFile);

    /**
     * @return The Resto collection name identifying FS-TEP reference data.
     */
    String getReferenceDataCollection();

    /**
     * @return The Resto collection name identifying FS-TEP output products.
     */
    String getOutputProductsCollection();
    
    /**
     * Creates a new Resto collection
     */
    boolean createOutputCollection(Collection collection);
    
    /**
     * Deletes a resto collection
     * @return 
     */
    boolean deleteCollection(Collection collection);
}
