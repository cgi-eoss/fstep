package com.cgi.eoss.fstep.model.projections;

import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

import com.cgi.eoss.fstep.model.Subscription;

/**
 * <p>Abbreviated representation of a Subscription entity, for embedding in REST responses.</p>
 */
@Projection(name = "shortSubscription", types = {Subscription.class})
public interface ShortSubscription extends Identifiable<Long> {
	ShortUser getOwner();
    
}
