package com.cgi.eoss.fstep.model.projections;

import com.cgi.eoss.fstep.security.FstepAccess;
import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.FstepServiceTemplate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

/**
 * <p>Default JSON projection for embedded {@link FstepServiceTemplate}s. Embeds the owner as a ShortUser.</p>
 */
@Projection(name = "shortFstepServiceTemplate", types = {FstepServiceTemplate.class})
public interface ShortFstepServiceTemplate extends Identifiable<Long> {
    String getName();
    String getDescription();
    FstepService.Type getType();
    ShortUser getOwner();
    @Value("#{@fstepSecurityService.getCurrentAccess(T(com.cgi.eoss.fstep.model.FstepServiceTemplate), target.id)}")
    FstepAccess getAccess();
}
