package com.cgi.eoss.fstep.api.mappings;

import com.cgi.eoss.fstep.catalogue.CatalogueUri;
import com.cgi.eoss.fstep.model.Databasket;
import com.cgi.eoss.fstep.model.projections.DetailedDatabasket;
import com.cgi.eoss.fstep.model.projections.ShortDatabasket;
import com.google.common.collect.ImmutableMap;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.stereotype.Component;

/**
 * <p>HATEOAS resource processor for {@link Databasket}s.</p>
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DatabasketResourceProcessor extends BaseResourceProcessor<Databasket> {

    private final RepositoryEntityLinks entityLinks;

    @Override
    protected EntityLinks getEntityLinks() {
        return entityLinks;
    }

    @Override
    protected Class<Databasket> getTargetClass() {
        return Databasket.class;
    }

    private void addFstepLink(Resource resource, Long databasketId) {
        resource.add(new Link(CatalogueUri.DATABASKET.build(ImmutableMap.of("id", String.valueOf(databasketId))).toASCIIString()).withRel("fstep"));
    }

    @Component
    private final class BaseEntityProcessor implements ResourceProcessor<Resource<Databasket>> {
        @Override
        public Resource<Databasket> process(Resource<Databasket> resource) {
            Databasket entity = resource.getContent();

            addSelfLink(resource, entity);
            addFstepLink(resource, entity.getId());

            return resource;
        }
    }

    @Component
    private final class DetailedEntityProcessor implements ResourceProcessor<Resource<DetailedDatabasket>> {
        @Override
        public Resource<DetailedDatabasket> process(Resource<DetailedDatabasket> resource) {
            DetailedDatabasket entity = resource.getContent();

            addSelfLink(resource, entity);
            addFstepLink(resource, entity.getId());

            return resource;
        }
    }

    @Component
    private final class ShortEntityProcessor implements ResourceProcessor<Resource<ShortDatabasket>> {
        @Override
        public Resource<ShortDatabasket> process(Resource<ShortDatabasket> resource) {
            ShortDatabasket entity = resource.getContent();

            addSelfLink(resource, entity);
            addFstepLink(resource, entity.getId());

            return resource;
        }
    }

}
