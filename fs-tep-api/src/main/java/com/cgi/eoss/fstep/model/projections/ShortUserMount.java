package com.cgi.eoss.fstep.model.projections;

import com.cgi.eoss.fstep.model.UserMount;
import com.cgi.eoss.fstep.security.FstepAccess;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

/**
 * <p>
 * Abbreviated representation of a UserMount entity, for embedding in REST responses.
 * </p>
 */
@Projection(name = "shortUserMount", types = {UserMount.class})
public interface ShortUserMount extends Identifiable<Long> {
    String getName();

    String getType();
    
    String getMountPath();

    ShortUser getOwner();

    @Value("#{@fstepSecurityService.getCurrentAccess(T(com.cgi.eoss.fstep.model.UserMount), target.id)}")
    FstepAccess getAccess();
}
