package com.cgi.eoss.fstep.api.mappings;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.stereotype.Component;

import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.internal.FstepFileIngestion;

import lombok.RequiredArgsConstructor;

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
		fstepFileResourceProcessor.addOGCLinks(resource, entity);
		fstepFileResourceProcessor.addFstepLink(resource, entity.getUri());

        return resource;
	}

}
