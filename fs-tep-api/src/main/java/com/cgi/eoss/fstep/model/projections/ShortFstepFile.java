package com.cgi.eoss.fstep.model.projections;

import com.cgi.eoss.fstep.security.FstepAccess;
import com.cgi.eoss.fstep.model.FstepFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

import java.util.UUID;

/**
 * <p>Abbreviated representation of an FstepFile entity, for embedding in REST responses.</p>
 */
@Projection(name = "shortFstepFile", types = {FstepFile.class})
public interface ShortFstepFile extends Identifiable<Long> {
    UUID getRestoId();
    ShortUser getOwner();
    String getFilename();
    FstepFile.Type getType();
    @Value("#{@fstepSecurityService.getCurrentAccess(T(com.cgi.eoss.fstep.model.FstepFile), target.id)}")
    FstepAccess getAccess();
}
