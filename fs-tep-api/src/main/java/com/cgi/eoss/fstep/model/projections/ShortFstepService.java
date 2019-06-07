package com.cgi.eoss.fstep.model.projections;

import com.cgi.eoss.fstep.security.FstepAccess;
import com.cgi.eoss.fstep.model.CostingExpression;
import com.cgi.eoss.fstep.model.FstepService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

/**
 * <p>Default JSON projection for embedded {@link FstepService}s. Embeds the owner as a ShortUser.</p>
 */
@Projection(name = "shortFstepService", types = {FstepService.class})
public interface ShortFstepService extends Identifiable<Long> {
    String getName();
    String getDescription();
    FstepService.Type getType();
    ShortUser getOwner();
    String getDockerTag();
    FstepService.Licence getLicence();
    FstepService.Status getStatus();
    @Value("#{@fstepSecurityService.getCurrentAccess(T(com.cgi.eoss.fstep.model.FstepService), target.id)}")
    FstepAccess getAccess();
    @Value("#{@costingService.getServiceCostingExpression(target)}")
    CostingExpression getCostingExpression();
}
