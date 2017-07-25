package com.cgi.eoss.fstep.model.projections;

import com.cgi.eoss.fstep.security.FstepAccess;
import com.cgi.eoss.fstep.model.FstepFile;
import org.geojson.GeoJsonObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

import java.net.URI;
import java.util.UUID;

/**
 * <p>Comprehensive representation of an FstepFile entity, including all catalogue metadata, for embedding in REST
 * responses.</p>
 */
@Projection(name = "detailedFstepFile", types = FstepFile.class)
public interface DetailedFstepFile extends Identifiable<Long> {
    URI getUri();
    UUID getRestoId();
    FstepFile.Type getType();
    ShortUser getOwner();
    String getFilename();
    @Value("#{@restoServiceImpl.getGeoJsonSafe(target)}")
    GeoJsonObject getMetadata();
    @Value("#{@fstepSecurityService.getCurrentAccess(T(com.cgi.eoss.fstep.model.FstepFile), target.id)}")
    FstepAccess getAccess();
}
