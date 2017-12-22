package com.cgi.eoss.fstep.model.projections;

import com.cgi.eoss.fstep.model.UserEndpoint;
import com.cgi.eoss.fstep.security.FstepAccess;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

/**
 * <p>
 * Abbreviated representation of a UserEndpoint entity, for embedding in REST responses.
 * </p>
 */
@Projection(name = "shortUserEndpoint", types = {UserEndpoint.class})
public interface ShortUserEndpoint extends Identifiable<Long> {
    String getName();
    
    String getUrl();

    ShortUser getOwner();

    @Value("#{@fstepSecurityService.getCurrentAccess(T(com.cgi.eoss.fstep.model.UserEndpoint), target.id)}")
    FstepAccess getAccess();
}
