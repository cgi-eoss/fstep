package com.cgi.eoss.fstep.catalogue.resto;

import com.cgi.eoss.fstep.model.FstepFile;
import org.geojson.GeoJsonObject;

import java.util.UUID;

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
     * <p>Ingest the given GeoJsonObject to the Resto catalogue, in the FS-TEP Output Project collection, and return the
     * new record's UUID.</p>
     */
    UUID ingestOutputProduct(GeoJsonObject object);

    /**
     * <p>Ingest the given GeoJsonObject to the Resto catalogue, in the given collection, and return the new record's
     * UUID.</p>
     */
    UUID ingestExternalProduct(String collection, GeoJsonObject object);

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
}
