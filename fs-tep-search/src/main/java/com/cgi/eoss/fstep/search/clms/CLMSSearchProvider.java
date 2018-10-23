package com.cgi.eoss.fstep.search.clms;

import java.io.IOException;
import java.math.RoundingMode;
import java.net.URI;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.geojson.Feature;
import org.springframework.core.io.Resource;
import org.springframework.hateoas.Link;
import org.springframework.web.util.UriComponentsBuilder;

import com.cgi.eoss.fstep.search.api.SearchParameters;
import com.cgi.eoss.fstep.search.api.SearchProvider;
import com.cgi.eoss.fstep.search.api.SearchResults;
import com.cgi.eoss.fstep.search.clms.model.CLMSCollection;
import com.cgi.eoss.fstep.search.clms.model.CLMSProduct;
import com.cgi.eoss.fstep.search.clms.model.CLMSResult;
import com.cgi.eoss.fstep.search.clms.model.Feed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * <p>Common parent class for search providers interacting with Resto catalogues.</p> <p>Provides adapters for
 * pagination and common search parameters.</p>
 */
@Log4j2
public class CLMSSearchProvider implements SearchProvider {

    private final HttpUrl baseUrl;
    private final OkHttpClient client;
    private final XmlMapper xmlMapper;
    private final ObjectMapper jsonMapper;
    
	
    public CLMSSearchProvider(HttpUrl baseUrl, OkHttpClient httpClient, XmlMapper xmlMapper, ObjectMapper jsonMapper) {
        this.baseUrl = baseUrl;
        this.client = httpClient;
        this.xmlMapper = xmlMapper;
        this.jsonMapper = jsonMapper;
    }

    private static final Set<String> INTERNAL_FSTEP_PARAMS = ImmutableSet.of(
            "catalogue",
            "mission",
            "platform"
    );
    private static final Map<String, String> PARAMETER_NAME_MAPPING = ImmutableMap.<String, String>builder()
            .put("productDateStart", "start")
            .put("productDateEnd", "end")
            .build();
    
    private static final Map<String, Function<String, String>> PARAMETER_VALUE_MAPPING = ImmutableMap.<String, Function<String, String>>builder()
             .build();
	@Override
	public int getPriority() {
		return 0;
	}

	@Override
	public SearchResults search(SearchParameters parameters) throws IOException {
		String collectionName = getCollection(parameters);

        HttpUrl.Builder httpUrl = baseUrl.newBuilder().addPathSegment("findProducts.json").addEncodedQueryParameter("collection", collectionName);
        getPagingParameters(parameters).forEach(httpUrl::addQueryParameter);
        
        getQueryParameters(parameters).forEach(httpUrl::addQueryParameter);

        Request request = new Request.Builder().url(httpUrl.build()).get().build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                LOG.error("Received unsuccessful HTTP response for CLMS search: {}", response.toString());
                throw new IOException("Unexpected HTTP response code from CLMS: " + response);
            }
            LOG.info("Received successful response for CLMS search: {}", request.url());
            CLMSResult clmsResult = jsonMapper.readValue(response.body().string(), CLMSResult.class);

            SearchResults.Page page = getPageInfo(parameters, clmsResult);
            return postProcess(SearchResults.builder()
                    .parameters(parameters)
                    .page(page)
                    .features(clmsResult.getProducts().stream().map(CLMSProduct::asFeature).collect(Collectors.toList()))
                    .links(getLinks(parameters.getRequestUrl(), page))
                    .build());
        }
	}

	private String getCollection(SearchParameters parameters) {
		Optional<String> collection = parameters.getValue("clmscollection");
        if (collection.isPresent()){
            return collection.get();
        }
        else {
            return getDynamicParameterDefaultValue("clmscollection");
        }
    }
	

	private SearchResults postProcess(SearchResults results) {
		results.getFeatures().forEach(f -> {
			try {
				addFstepProperties(f, results.getParameters());
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}
		});
        return results;
    }
	
	private void addFstepProperties(Feature feature, SearchParameters parameters) throws ParseException {
		String productSource = "clms";
        String productIdentifier = feature.getProperty("identifier");
        UriComponentsBuilder builder = UriComponentsBuilder.newInstance().scheme(productSource).host("collection-product").pathSegment(getCollection(parameters)).pathSegment(productIdentifier);
        //Attach AOI to url
        List<String> aoiParam = parameters.getParameters().get("aoi");
        if (aoiParam.size() > 0 && !Strings.isNullOrEmpty(aoiParam.get(0))) {
        	String aoiStr = aoiParam.get(0);
        	//Convert aoi to bbox
        	WKTReader wktReader = new WKTReader();
        	Geometry aoi = wktReader.read(aoiStr);
        	Envelope bbox = aoi.getEnvelopeInternal();
        	StringBuilder sb = new StringBuilder();
        	DecimalFormat fourPlacesDecimalFormat = new DecimalFormat(".####");
        	fourPlacesDecimalFormat.setRoundingMode(RoundingMode.FLOOR);
        	sb.append(fourPlacesDecimalFormat.format(bbox.getMinX())).append('%').append(fourPlacesDecimalFormat.format(bbox.getMinY())).append('%');
        	fourPlacesDecimalFormat.setRoundingMode(RoundingMode.CEILING);
        	sb.append(fourPlacesDecimalFormat.format(bbox.getMaxX())).append('%').append(fourPlacesDecimalFormat.format(bbox.getMaxY()));
        	builder.queryParam("bbox", sb.toString());
        }
        URI fstepUri = builder.build().toUri();
        
        // Shuffle the CLMS properties into a sub-object for consistency across all search providers
        Map<String, Object> sourceProperties = new HashMap<>(feature.getProperties());
        feature.getProperties().clear();

        Set<Link> featureLinks = new HashSet<>();
        featureLinks.add(new Link(fstepUri.toASCIIString(), "fstep"));
        //TODO is there any file size for CLMS products?
        Long filesize = Optional.ofNullable((Map<String, Map<String, Object>>) sourceProperties.get("services"))
                .map(services -> Optional.ofNullable(services.get("download")).map(dl -> ((Number) dl.get("size")).longValue()).orElse(0L))
                .orElse(0L);

        // Required parameters for FstepFile ingestion
        feature.setProperty("productSource", productSource);
        feature.setProperty("productIdentifier", productIdentifier);
        feature.setProperty("fstepUrl", fstepUri);
        feature.setProperty("filesize", filesize);
        feature.setProperty("fstepUsable", true);

        // Set "interesting" parameters which clients might want in an easily-accessible form
        // Some are not present depending on the result type, so we have to safely traverse the dynamic properties map
        // These are added to extraParams so that the FstepFile/Resto schema is predictable
        Map<String, Object> targetProperties = new HashMap<>();
        
        Optional.ofNullable(sourceProperties.get("beginPosition"))
        .ifPresent(startDate -> targetProperties.put("fstepStartTime", startDate));
        Optional.ofNullable(sourceProperties.get("endPosition"))
        .ifPresent(startDate -> targetProperties.put("fstepEndTime", startDate));
        feature.setProperty("extraParams", targetProperties);
        
        Object quicklookURL = sourceProperties.get("quicklookLink");
        if (quicklookURL != null) {
        		featureLinks.add(new Link(quicklookURL.toString(), "quicklook"));
        }
        feature.setProperty("_links", featureLinks.stream().collect(Collectors.toMap(
                Link::getRel,
                l -> ImmutableMap.of("href", l.getHref())
        )));
	}

	private Map<String, SearchResults.Link> getLinks(HttpUrl requestUrl, SearchResults.Page page) {
        Map<String, SearchResults.Link> links = new HashMap<>();

        links.putAll(getPagingLinks(page, requestUrl));

        return links;
    }
	
	@Override
    public Map<String, String> getPagingParameters(SearchParameters parameters) {
        Map<String, String> pagingParameters = new HashMap<>();
        pagingParameters.put("count", Integer.toString(parameters.getResultsPerPage()));
        pagingParameters.put("startPage", Integer.toString(parameters.getPage() + 1));
        return pagingParameters;
    }

	@Override
    public Map<String, String> getQueryParameters(SearchParameters parameters) {
        Map<String, String> queryParameters = new HashMap<>();

        parameters.getParameters().asMap().entrySet().stream()
                .filter(p -> !INTERNAL_FSTEP_PARAMS.contains(p.getKey()) && !p.getValue().isEmpty() && !Strings.isNullOrEmpty(Iterables.getFirst(p.getValue(), null)))
                .forEach(p -> addTransformedParameter(queryParameters, p));

        return queryParameters;
    }
	
	private void addTransformedParameter(Map<String, String> queryParameters, Map.Entry<String, Collection<String>> parameter) {
        String parameterName = parameter.getKey();
        String parameterValue = Iterables.getFirst(parameter.getValue(), null);

        queryParameters.put(
                Optional.ofNullable(PARAMETER_NAME_MAPPING.get(parameterName)).orElse(parameterName),
                Optional.ofNullable(PARAMETER_VALUE_MAPPING.get(parameterName)).map(f -> f.apply(parameterValue)).orElse(parameterValue)
        );
    }

	@Override
	public boolean supports(SearchParameters parameters) {
		String catalogue = parameters.getValue("catalogue", "UNKNOWN");
		return catalogue.equals("CLMS_DATA");
	}

	@Override
	public boolean supportsQuicklook(String productSource, String productIdentifier) {
		return false;
	}

	@Override
	public Resource getQuicklook(String productSource, String productIdentifier) throws IOException {
		return null;
	}

	@Override
    public boolean supportsDynamicParameter(String parameter) {
        return "clmscollection".equals(parameter);
    }

	@Override
    public List<Map<String, Object>> getDynamicParameterValues(String parameter){
        if (parameter.equals("clmscollection")) {
        	List<CLMSCollection> collections;
			try {
				collections = getCLMSCollections(null);
				return collections.stream()
			            .map(collection -> new HashMap<String, Object>() {{
			                put("title", collection.getSummary());
			                put("value", collection.getTitle());
			                }})
			            .collect(Collectors.toList());
			} catch (IOException e) {
				LOG.error("Unsuccessful communication with CLMS service", e);
			}
        }
        return Collections.<Map<String, Object>>emptyList();
    }

	private List<CLMSCollection> getCLMSCollections(Integer count) throws IOException {
		HttpUrl.Builder httpUrl = baseUrl.newBuilder().addPathSegment("findCollections");
		if (count != null) {
			baseUrl.newBuilder().addQueryParameter("count", count.toString());
		}
		Request request = new Request.Builder().url(httpUrl.build()).get().build();
		try (Response response = client.newCall(request).execute()) {
		    if (!response.isSuccessful()) {
		        LOG.error("Received unsuccessful HTTP response for CLMS collections search: {}", response.toString());
		        throw new IOException("Unexpected HTTP response code from CLMS: " + response);
		    }
		    LOG.info("Received successful response for CLMS collections search: {}", request.url());
		    Feed f = (Feed) xmlMapper.readValue(response.body().string(), Feed.class);
		    return f.getCollections();
		}
	}

	@Override
	public String getDynamicParameterDefaultValue(String parameter) {
		if (parameter.equals("clmscollection")) {
        	try {
				return getCLMSCollections(1).get(0).getTitle();
			} catch (IOException e) {
				return null;
			}
        }
        return null;
	}
	
	private SearchResults.Page getPageInfo(SearchParameters parameters, CLMSResult clmsResult) {
        long queryResultsPerPage = parameters.getResultsPerPage();
        long queryPage = parameters.getPage();

        long totalResultsCount = clmsResult.getTotalResults();
        long countOnPage = clmsResult.getItemsPerPage();
        long totalPages = (totalResultsCount / queryResultsPerPage) + 1;

        return SearchResults.Page.builder()
                .totalElements(totalResultsCount)
                .size(countOnPage)
                .number(queryPage)
                .totalPages(totalPages)
                .build();
    }
	
	protected Map<String, SearchResults.Link> getPagingLinks(SearchResults.Page page, HttpUrl requestUrl) {
        Map<String, SearchResults.Link> links = new HashMap<>();
        links.put("first", getPageLink("first", requestUrl, 0));
        if (page.getNumber() > 0) {
            links.put("prev", getPageLink("prev", requestUrl, page.getNumber() - 1));
        }
        if (page.getNumber() < page.getTotalPages() - 1) {
            links.put("next", getPageLink("next", requestUrl, page.getNumber() + 1));
        }
        links.put("last", getPageLink("last", requestUrl, page.getTotalPages() - 1));
        return links;
    }
	
	private SearchResults.Link getPageLink(String rel, HttpUrl requestUrl, long page) {
        return SearchResults.Link.builder().rel(rel).href(requestUrl.newBuilder().removeAllQueryParameters("page").addQueryParameter("page", String.valueOf(page)).build().toString()).build();
    }

}