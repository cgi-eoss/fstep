package com.cgi.eoss.fstep.search.fstep;

import com.cgi.eoss.fstep.catalogue.CatalogueService;
import com.cgi.eoss.fstep.catalogue.resto.RestoService;
import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.persistence.service.FstepFileDataService;
import com.cgi.eoss.fstep.search.api.SearchParameters;
import com.cgi.eoss.fstep.search.api.SearchResults;
import com.cgi.eoss.fstep.search.resto.RestoResult;
import com.cgi.eoss.fstep.search.resto.RestoSearchProvider;
import com.cgi.eoss.fstep.security.FstepSecurityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import lombok.extern.log4j.Log4j2;
import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import org.springframework.core.io.Resource;
import org.springframework.hateoas.Link;

import java.io.IOException;
import java.net.URI;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
public class FstepSearchProvider extends RestoSearchProvider {

    private final int priority;
    private final CatalogueService catalogueService;
    private final RestoService restoService;
    private final FstepFileDataService fstepFileDataService;
    private final FstepSecurityService securityService;

    public FstepSearchProvider(int priority, FstepSearchProperties searchProperties, OkHttpClient httpClient, ObjectMapper objectMapper, CatalogueService catalogueService, RestoService restoService, FstepFileDataService fstepFileDataService, FstepSecurityService securityService) {
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

        return queryParameters;
    }

    @Override
    public boolean supports(SearchParameters parameters) {
        String catalogue = parameters.getValue("catalogue", "UNKNOWN");
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
        switch (parameters.getValue("catalogue").orElse("")) {
            case "REF_DATA":
                return restoService.getReferenceDataCollection();
            case "FSTEP_PRODUCTS":
                return restoService.getOutputProductsCollection();
            default:
                throw new IllegalArgumentException("Could not identify Resto collection for repo type: " + parameters.getValue("catalogue"));
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
                    filesize = fstepFile.getFilesize();

                    if (fstepUsable) {
                        HttpUrl.Builder downloadUrlBuilder = parameters.getRequestUrl().newBuilder();
                        parameters.getRequestUrl().queryParameterNames().forEach(downloadUrlBuilder::removeAllQueryParameters);
                        downloadUrlBuilder.addPathSegment("dl").addPathSegment("fstep").addPathSegment(String.valueOf(fstepFile.getId()));
                        featureLinks.add(new Link(downloadUrlBuilder.build().toString(), "download"));

                        HttpUrl wmsLink = catalogueService.getWmsUrl(fstepFile.getType(), fstepFile.getUri());
                        if (wmsLink != null) {
                            featureLinks.add(new Link(wmsLink.toString(), "wms"));
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

            if (results.getParameters().getValue("catalogue", "").equals("REF_DATA")) {
                // Reference data timestamp is just the publish time
                extraParams.put("fstepStartTime", ZonedDateTime.parse(f.getProperty("published")).with(ZoneOffset.UTC).toLocalDateTime());
                extraParams.put("fstepEndTime", ZonedDateTime.parse(f.getProperty("published")).with(ZoneOffset.UTC).toLocalDateTime());
            } else if (results.getParameters().getValue("catalogue", "").equals("FSTEP_PRODUCTS")) {
                // FS-TEP products should have jobStart/EndDate
                extraParams.put("fstepStartTime", ZonedDateTime.parse(f.getProperty("jobStartDate")).with(ZoneOffset.UTC).toLocalDateTime());
                extraParams.put("fstepEndTime", ZonedDateTime.parse(f.getProperty("jobEndDate")).with(ZoneOffset.UTC).toLocalDateTime());
            }

            f.getProperties().put("extraParams", extraParams);

            // FS-TEP links are "_links", resto links are "links"
            f.setProperty("_links", featureLinks.stream().collect(Collectors.toMap(
                    Link::getRel,
                    l -> ImmutableMap.of("href", l.getHref())
            )));
        });
        return results;
    }

}
