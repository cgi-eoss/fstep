package com.cgi.eoss.fstep.model.projections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

import com.cgi.eoss.fstep.model.SystematicProcessing;
import com.cgi.eoss.fstep.security.FstepAccess;

/**
 * <p>Default JSON projection for embedded {@link SystematicProcessing}s. Embeds the owner as a ShortUser.</p>
 */
@Projection(name = "shortSystematicProcessing", types = {SystematicProcessing.class})
public interface ShortSystematicProcessing extends Identifiable<Long> {
    ShortUser getOwner();
    SystematicProcessing.Status getStatus();
    @Value("#{@fstepSecurityService.getCurrentAccess(T(com.cgi.eoss.fstep.model.Job), target.id)}")
    FstepAccess getAccess();
}
