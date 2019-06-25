package com.cgi.eoss.fstep.model.projections;

import com.cgi.eoss.fstep.model.Collection;
import com.cgi.eoss.fstep.model.CostingExpression;
import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.security.FstepAccess;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

/**
 * <p>Abbreviated representation of a Collection entity, for embedding in REST responses.</p>
 */
@Projection(name = "shortCollection", types = {Collection.class})
public interface ShortCollection extends Identifiable<Long> {
    String getName();
    String getIdentifier();
    String getDescription();
    FstepFile.Type getFileType();
    String getProductsType();
    ShortUser getOwner();
    @Value("#{@fstepSecurityService.getCurrentAccess(T(com.cgi.eoss.fstep.model.Collection), target.id)}")
    FstepAccess getAccess();
    @Value("#{@costingService.getCollectionCostingExpression(target)}")
    CostingExpression getCostingExpression();
}
