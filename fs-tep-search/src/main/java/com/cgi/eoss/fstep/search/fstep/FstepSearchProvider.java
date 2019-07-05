package com.cgi.eoss.fstep.search.fstep;

import com.cgi.eoss.fstep.catalogue.CatalogueService;
import com.cgi.eoss.fstep.catalogue.resto.RestoService;
import com.cgi.eoss.fstep.model.Collection;
import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.FstepFile.Type;
import com.cgi.eoss.fstep.persistence.service.CollectionDataService;
import com.cgi.eoss.fstep.persistence.service.FstepFileDataService;
import com.cgi.eoss.fstep.search.api.SearchParameters;
import com.cgi.eoss.fstep.search.api.SearchResults;
import com.cgi.eoss.fstep.search.resto.RestoResult;
import com.cgi.eoss.fstep.search.resto.RestoSearchProvider;
import com.cgi.eoss.fstep.security.FstepSecurityService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;

import lombok.extern.log4j.Log4j2;
import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.HttpUrl.Builder;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import org.apache.commons.lang.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.hateoas.Link;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors; 

@Log4j2
public class FstepSearchProvider extends RestoSearchProvider {

    private static final String CATALOGUE_PARAMETER = "catalogue";
	private static final String OUTPUT_COLLECTION_PARAMETER = "collection";
	private static final String REFERENCE_DATA_COLLECTION_PARAMETER = "refDataCollection";
	private final int priority;
    private final CatalogueService catalogueService;
    private final RestoService restoService;
    private final FstepFileDataService fstepFileDataService;
    private final FstepSecurityService securityService;
    private final CollectionDataService collectionDataService;
    private final ObjectMapper unwrappedArrayObjectMapper;
    
    public FstepSearchProvider(int priority, FstepSearchProperties searchProperties, OkHttpClient httpClient, ObjectMapper objectMapper, CatalogueService catalogueService, RestoService restoService, FstepFileDataService fstepFileDataService, FstepSecurityService securityService, CollectionDataService collectionDataService) {
        super(searchProperties.getBaseUrl(),
                httpClient.newBuilder()
                        .addInterceptor(chain -> {
                            Request authenticatedRequest = chain.request().newBuilder()
                                    .header("Authorization", Credentials.basic(searchProperties.getUsername(), searchProperties.getPassword()))
                                    .build();
                            return chain.proceed(authenticatedRequest);
                        })
                        .addInterceptor(new HttpLoggingInterceptor(LOG::trace).setLevel(HttpLoggingInterceptor.Level.BODY))
                        .build(),
                objectMapper);
        this.priority = priority;
        this.catalogueService = catalogueService;
        this.restoService = restoService;
        this.fstepFileDataService = fstepFileDataService;
        this.securityService = securityService;
        this.collectionDataService = collectionDataService;
        unwrappedArrayObjectMapper = new ObjectMapper();
        unwrappedArrayObjectMapper.enable(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED);
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public Map<String, String> getQueryParameters(SearchParameters parameters) {
        Map<String, String> queryParameters = new HashMap<>();

        parameters.getValue("identifier").ifPresent(s -> queryParameters.put("productIdentifier", "%" + s + "%"));
        parameters.getValue("aoi").ifPresent(s -> queryParameters.put("geometry", s));
        parameters.getValue("owner").ifPresent(s -> queryParameters.put("owner", s));
        parameters.getValue("productDateStart").ifPresent(s -> queryParameters.put("startDate", s));
        parameters.getValue("productDateEnd").ifPresent(s -> queryParameters.put("completionDate", s));
        parameters.getValue("jobDateStart").ifPresent(s -> queryParameters.put("jobStartDate", s));
        parameters.getValue("jobDateEnd").ifPresent(s -> queryParameters.put("jobEndDate", s));
        parameters.getValue("publicationDateStart").ifPresent(s -> queryParameters.put("publishedAfter", s));
        parameters.getValue("publicationDateEnd").ifPresent(s -> queryParameters.put("publishedBefore", s));
        parameters.getValue("fstepparam").ifPresent(s -> queryParameters.put("fstepparam", s));
        parameters.getValue("sortParam").ifPresent(s -> queryParameters.put("sortParam", s));
        parameters.getValue("sortOrder").ifPresent(s -> queryParameters.put("sortOrder", s));
        return queryParameters;
    }

    @Override
    public boolean supports(SearchParameters parameters) {
        String catalogue = parameters.getValue(CATALOGUE_PARAMETER, "UNKNOWN");
        return catalogue.equals("REF_DATA") || catalogue.equals("FSTEP_PRODUCTS");
    }

    @Override
    public boolean supportsQuicklook(String productSource, String productIdentifier) {
        return false;
    }

    @Override
    public Resource getQuicklook(String productSource, String productIdentifier) throws IOException {
        throw new IOException("Not implemented");
    }

    @Override
    protected Map<String, SearchResults.Link> getLinks(HttpUrl requestUrl, SearchResults.Page page, RestoResult restoResult) {
        Map<String, SearchResults.Link> links = new HashMap<>();

        links.putAll(getPagingLinks(page, requestUrl));

        return links;
    }

    @Override
    protected String getCollection(SearchParameters parameters) {
    	switch (parameters.getValue(CATALOGUE_PARAMETER).orElse("")) {
        case "REF_DATA":
        	return parameters.getValue(REFERENCE_DATA_COLLECTION_PARAMETER).orElse(restoService.getReferenceDataCollection());
        case "FSTEP_PRODUCTS":
        	return parameters.getValue(OUTPUT_COLLECTION_PARAMETER).orElse(restoService.getOutputProductsCollection());
        default:
            throw new IllegalArgumentException("Could not identify Resto collection for repo type: " + parameters.getValue(CATALOGUE_PARAMETER));
    	}
    }

    @SuppressWarnings("unchecked")
    @Override
    protected SearchResults postProcess(SearchResults results) {
        SearchParameters parameters = results.getParameters();
        // Add visibility info, if the result can be matched to an FstepFile
        results.getFeatures().forEach(f -> {
            // Default to usable
            boolean fstepUsable = true;
            URI fstepUri = null;
            Long filesize = null;

            // TODO Migrate to Spring Data Rest somehow?
            Set<Link> featureLinks = new HashSet<>();

            try {
                FstepFile fstepFile = fstepFileDataService.getByRestoId(UUID.fromString(f.getId()));
                if (fstepFile != null) {
                    fstepUsable = securityService.isReadableByCurrentUser(FstepFile.class, fstepFile.getId());
                    fstepUri = fstepFile.getUri();
                    featureLinks.add(new Link(fstepUri.toASCIIString(), "fstep"));
                    Builder productUrlBuilder = parameters.getRequestUrl().newBuilder();
                    parameters.getRequestUrl().queryParameterNames().forEach(productUrlBuilder::removeAllQueryParameters);
                    productUrlBuilder.addPathSegment("products").addPathSegment("fstep").addPathSegment(String.valueOf(fstepFile.getId()));
                    featureLinks.add(new Link(productUrlBuilder.build().toString(), "fstepFile"));
                    filesize = fstepFile.getFilesize();

                    if (fstepUsable) {
                        HttpUrl.Builder downloadUrlBuilder = parameters.getRequestUrl().newBuilder();
                        parameters.getRequestUrl().queryParameterNames().forEach(downloadUrlBuilder::removeAllQueryParameters);
                        downloadUrlBuilder.addPathSegment("dl").addPathSegment("fstep").addPathSegment(String.valueOf(fstepFile.getId()));
                        featureLinks.add(new Link(downloadUrlBuilder.build().toString(), "download"));
                        Set<Link> ogcLinks = catalogueService.getOGCLinks(fstepFile);
                        if (ogcLinks != null) {
                            featureLinks.addAll(ogcLinks);
                        }
                        
                    }
                } else {
                    LOG.debug("No FstepFile found for search result with ID: {}", f.getId());
                }
            } catch (Exception e) {
                LOG.debug("Could not check visibility of search result with ID: {}", f.getId(), e);
            }
            f.getProperties().put("fstepUsable", fstepUsable);
            f.getProperties().put("fstepUrl", fstepUri);
            f.getProperties().put("filesize", filesize);


            Map<String, Object> extraParams = Optional.ofNullable((Map<String, Object>) f.getProperties().get("extraParams")).orElse(new HashMap<>());
            
            Optional.ofNullable(f.getProperty("startDate"))
            .ifPresent(startDate -> extraParams.put("fstepStartTime", startDate));
            Optional.ofNullable(f.getProperty("completionDate"))
            .ifPresent(completionDate -> extraParams.put("fstepEndTime", completionDate));
            
            f.getProperties().put("extraParams", extraParams);

            ImmutableListMultimap<String, Link> relToLinkMultimap = Multimaps.index(featureLinks, Link::getRel);
            ListMultimap<String, Map<String, String>> relToHrefsMultimap = Multimaps.transformValues(relToLinkMultimap, l -> ImmutableMap.of("href", l.getHref()));
            try {
                // FS-TEP links are "_links", resto links are "links"
                f.setProperty("_links", new RawJsonString(unwrappedArrayObjectMapper.writeValueAsString(relToHrefsMultimap.asMap())));
            } catch (JsonProcessingException e) {
                LOG.debug("Could not convert feature links to JSON: {}", f.getId(), e);
            }
        });
        return results;
    }
    
    @Override
    public boolean supportsDynamicParameter(String parameter) {
        return ImmutableList.of(OUTPUT_COLLECTION_PARAMETER, REFERENCE_DATA_COLLECTION_PARAMETER).contains(parameter);
    }
    
    @Override
    public List<Map<String, Object>> getDynamicParameterValues(String parameter){
        switch (parameter) {
        	case OUTPUT_COLLECTION_PARAMETER: return getVisibleCollectionsByFileType(Type.OUTPUT_PRODUCT);
        	case REFERENCE_DATA_COLLECTION_PARAMETER: return getVisibleCollectionsByFileType(Type.REFERENCE_DATA);
        	default: return Collections.emptyList();
        }
    }

    private List<Map<String, Object>> getVisibleCollectionsByFileType(Type fileType) {
		return collectionDataService.findByFileType(fileType).stream()
		.filter(collection -> securityService.isReadableByCurrentUser(Collection.class, collection.getId()))
		.map(this::collectionToMap)
		.collect(Collectors.toList());
	}
    
    private Map<String, Object> collectionToMap(Collection collection){
		HashMap<String, Object> collectionMap = new HashMap<>();
		collectionMap.put("title", collection.getName());
		collectionMap.put("value", collection.getIdentifier());
		collectionMap.put("description", collection.getDescription());
		return collectionMap;
	}
    
    @Override
    public String getDynamicParameterDefaultValue(String parameter){
    	switch (parameter) {
    		case OUTPUT_COLLECTION_PARAMETER: return getDefaultCollectionByFileType(Type.OUTPUT_PRODUCT, catalogueService.getDefaultOutputProductCollection())
        		.orElse(StringUtils.EMPTY);
    		case REFERENCE_DATA_COLLECTION_PARAMETER: return getDefaultCollectionByFileType(Type.REFERENCE_DATA, catalogueService.getDefaultReferenceDataCollection())
    			.orElse(StringUtils.EMPTY);
    		default: return null;
    	}
    }

	private Optional<String> getDefaultCollectionByFileType(Type fileType, String defaultCollectionForType) {
		List<Collection> readableCollections = collectionDataService.findByFileType(fileType).stream()
		.filter(collection -> securityService.isReadableByCurrentUser(Collection.class, collection.getId()))
		.collect(Collectors.toList());
		if (readableCollections.isEmpty()) {
		    return Optional.empty();
		}
		return Optional.of(readableCollections.stream()
		.filter(c -> c.getIdentifier().equals(defaultCollectionForType))
		.findFirst().orElse(readableCollections.get(0)).getName());
	}


}
