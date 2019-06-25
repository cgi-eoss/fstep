package com.cgi.eoss.fstep.model.projections;

import com.cgi.eoss.fstep.model.Collection;
import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.security.FstepAccess;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

/**
 * <p>Abbreviated representation of a Collection entity, for embedding in REST responses.</p>
 */
@Projection(name = "detailedCollection", types = {Collection.class})
public interface DetailedCollection extends Identifiable<Long> {
    String getName();
    String getIdentifier();
    String getDescription();
    String getProductsType();
    FstepFile.Type getFileType();
    ShortUser getOwner();
    @Value("#{target.fstepFiles.size()}")
    Integer getSize();
    @Value("#{@fstepSecurityService.getCurrentAccess(T(com.cgi.eoss.fstep.model.Collection), target.id)}")
    FstepAccess getAccess();
}
