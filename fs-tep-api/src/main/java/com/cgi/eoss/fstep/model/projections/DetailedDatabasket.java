package com.cgi.eoss.fstep.model.projections;

import com.cgi.eoss.fstep.security.FstepAccess;
import com.cgi.eoss.fstep.model.Databasket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

import java.util.Set;

/**
 * <p>Comprehensive representation of a Databasket entity, including all FstepFile catalogue metadata, for embedding in
 * REST responses.</p>
 */
@Projection(name = "detailedDatabasket", types = Databasket.class)
public interface DetailedDatabasket extends Identifiable<Long> {
    String getName();
    String getDescription();
    ShortUser getOwner();
    Set<ShortFstepFile> getFiles();
    @Value("#{@fstepSecurityService.getCurrentAccess(T(com.cgi.eoss.fstep.model.Databasket), target.id)}")
    FstepAccess getAccess();
}
