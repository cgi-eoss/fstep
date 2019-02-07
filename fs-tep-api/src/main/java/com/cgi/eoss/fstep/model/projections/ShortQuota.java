package com.cgi.eoss.fstep.model.projections;

import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

import com.cgi.eoss.fstep.model.Quota;
import com.cgi.eoss.fstep.model.UsageType;

/**
 * <p>Abbreviated representation of a Quota entity, for embedding in REST responses.</p>
 */
@Projection(name = "shortQuota", types = {Quota.class})
public interface ShortQuota extends Identifiable<Long> {
    ShortUser getOwner();
    Long getValue();
    UsageType getUsageType();
    
}
