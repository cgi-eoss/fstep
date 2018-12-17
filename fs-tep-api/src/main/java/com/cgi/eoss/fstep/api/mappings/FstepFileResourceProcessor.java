package com.cgi.eoss.fstep.api.mappings;

import com.cgi.eoss.fstep.catalogue.CatalogueService;
import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.projections.DetailedFstepFile;
import com.cgi.eoss.fstep.model.projections.ShortFstepFile;
import lombok.RequiredArgsConstructor;
import okhttp3.HttpUrl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.stereotype.Component;

import java.net.URI;

/**
 * <p>HATEOAS resource processor for {@link FstepFile}s. Adds extra _link entries for client use, e.g. file download.</p>
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class FstepFileResourceProcessor extends BaseResourceProcessor<FstepFile> {

    private final RepositoryEntityLinks entityLinks;
    private final CatalogueService catalogueService;

    @Override
    protected EntityLinks getEntityLinks() {
        return entityLinks;
    }

    @Override
    protected Class<FstepFile> getTargetClass() {
        return FstepFile.class;
    }

    void addDownloadLink(Resource resource, FstepFile.Type type) {
        // TODO Check file datasource?
        if (type != FstepFile.Type.EXTERNAL_PRODUCT) {
            // TODO Do this properly with a method reference
            resource.add(new Link(resource.getLink("self").getHref() + "/dl").withRel("download"));
        }
    }

    void addWmsLink(Resource resource, FstepFile.Type type, URI uri) {
        HttpUrl wmsLink = catalogueService.getWmsUrl(type, uri);
        if (wmsLink != null) {
            resource.add(new Link(wmsLink.toString()).withRel("wms"));
        }
    }

    void addFstepLink(Resource resource, URI fstepFileUri) {
        resource.add(new Link(fstepFileUri.toASCIIString()).withRel("fstep"));
    }

    @Component
    private final class BaseEntityProcessor implements ResourceProcessor<Resource<FstepFile>> {
        @Override
        public Resource<FstepFile> process(Resource<FstepFile> resource) {
            FstepFile entity = resource.getContent();

            addSelfLink(resource, entity);
            addDownloadLink(resource, entity.getType());
            addWmsLink(resource, entity.getType(), entity.getUri());
            addFstepLink(resource, entity.getUri());

            return resource;
        }
    }

    @Component
    private final class DetailedEntityProcessor implements ResourceProcessor<Resource<DetailedFstepFile>> {
        @Override
        public Resource<DetailedFstepFile> process(Resource<DetailedFstepFile> resource) {
            DetailedFstepFile entity = resource.getContent();

            addSelfLink(resource, entity);
            addDownloadLink(resource, entity.getType());
            addWmsLink(resource, entity.getType(), entity.getUri());
            addFstepLink(resource, entity.getUri());

            return resource;
        }
    }

    @Component
    private final class ShortEntityProcessor implements ResourceProcessor<Resource<ShortFstepFile>> {
        @Override
        public Resource<ShortFstepFile> process(Resource<ShortFstepFile> resource) {
            ShortFstepFile entity = resource.getContent();

            addSelfLink(resource, entity);
            addDownloadLink(resource, entity.getType());

            return resource;
        }
    }

}
