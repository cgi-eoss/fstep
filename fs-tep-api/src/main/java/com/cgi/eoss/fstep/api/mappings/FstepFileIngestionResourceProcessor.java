package com.cgi.eoss.fstep.api.mappings;

import com.cgi.eoss.fstep.catalogue.CatalogueService;
import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.internal.FstepFileIngestion;
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
 * <p>HATEOAS resource processor for {@link FstepFileIngestion}s. Adds extra _link entries for client use, e.g. file download.</p>
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class FstepFileIngestionResourceProcessor implements ResourceProcessor<Resource<FstepFileIngestion>> {

	private final FstepFileResourceProcessor fstepFileResourceProcessor;
	
	@Override
	public Resource<FstepFileIngestion> process(Resource<FstepFileIngestion> resource) {
		FstepFile entity = resource.getContent().getFstepFile();

		fstepFileResourceProcessor.addSelfLink(resource, entity);
		fstepFileResourceProcessor.addDownloadLink(resource, entity.getType());
		fstepFileResourceProcessor.addWmsLink(resource, entity.getType(), entity.getUri());
		fstepFileResourceProcessor.addFstepLink(resource, entity.getUri());

        return resource;
	}

}
