package com.cgi.eoss.fstep.search.creodias;

import com.cgi.eoss.fstep.catalogue.external.ExternalProductDataService;
import com.cgi.eoss.fstep.search.api.SearchParameters;
import com.cgi.eoss.fstep.search.api.SearchResults;
import com.cgi.eoss.fstep.search.resto.RestoResult;
import com.cgi.eoss.fstep.search.resto.RestoSearchProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import lombok.Builder;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import org.apache.commons.lang.StringUtils;
import org.geojson.Feature;
import org.springframework.core.io.Resource;
import org.springframework.hateoas.Link;
import java.io.IOException;
import java.net.URI;
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

@Log4j2
public class CreoDIASSearchProvider extends RestoSearchProvider {

    @Data
    @Builder
    private static final class MissionPlatform {
        private final String mission;
        private final String platform;
    }

    private static final BiMap<MissionPlatform, String> SUPPORTED_MISSIONS = ImmutableBiMap.<MissionPlatform, String>builder()
            .put(MissionPlatform.builder().mission("envisat").platform(null).build(), "Envisat")
            .put(MissionPlatform.builder().mission("landsat").platform("Landsat-5").build(), "Landsat5")
            .put(MissionPlatform.builder().mission("landsat").platform("Landsat-7").build(), "Landsat7")
            .put(MissionPlatform.builder().mission("landsat").platform("Landsat-8").build(), "Landsat8")
            .put(MissionPlatform.builder().mission("sentinel1").platform(null).build(), "Sentinel1")
            .put(MissionPlatform.builder().mission("sentinel2").platform(null).build(), "Sentinel2")
            .put(MissionPlatform.builder().mission("sentinel3").platform(null).build(), "Sentinel3")
            .put(MissionPlatform.builder().mission("sentinel5p").platform(null).build(), "Sentinel5P")
            .put(MissionPlatform.builder().mission("smos").platform(null).build(), "SMOS")
            .build();
    private static final Set<String> INTERNAL_FSTEP_PARAMS = ImmutableSet.of(
            "catalogue",
            "mission",
            "platform"
    );
    private static final Map<String, String> PARAMETER_NAME_MAPPING = ImmutableMap.<String, String>builder()
            .put("semantic", "q")
            .put("aoi", "geometry")
            .put("s1ProcessingLevel", "processingLevel")
            .put("s2ProcessingLevel", "processingLevel")
            .put("s3ProcessingLevel", "processingLevel")
            .put("s5pProcessingLevel", "processingLevel")
            .put("smosProcessingLevel", "processingLevel")
            .put("s3Instrument", "instrument")
            .put("s1ProductType", "productType")
            .put("s5pProductType", "productType")
            .put("smosProductType", "productType")
            .put("productDateStart", "startDate")
            .put("productDateEnd", "completionDate")
            .put("maxCloudCover", "cloudCover")
            .put("identifier", "productIdentifier")
            .build();
    private static final Map<String, Function<String, String>> PARAMETER_VALUE_MAPPING = ImmutableMap.<String, Function<String, String>>builder()
            .put("identifier", v -> "%" + v + "%")
            .put("maxCloudCover", v -> "[0,"+ v +"]")
            .put("s1ProcessingLevel", v -> "LEVEL"+v)
            .put("s2ProcessingLevel", v -> "LEVEL"+v.substring(1))
            .put("s3ProcessingLevel", v -> "LEVEL"+v)
            .put("s5pProcessingLevel",  v -> "LEVEL"+v)
            .put("s3Instrument", v-> v.substring(0, 2))
            .build();

    private final int priority;
    private final ExternalProductDataService externalProductService;

    public CreoDIASSearchProvider(int priority, CreoDIASSearchProperties searchProperties, OkHttpClient httpClient, ObjectMapper objectMapper, ExternalProductDataService externalProductService) {
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
        this.externalProductService = externalProductService;
    }

    @Override
    public int getPriority() {
        return priority;
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
        return catalogue.equals("SATELLITE") && SUPPORTED_MISSIONS.containsKey(getMissionPlatform(parameters));
    }

    @Override
    public boolean supportsQuicklook(String productSource, String productIdentifier) {
        return SUPPORTED_MISSIONS.keySet().stream().map(MissionPlatform::getMission).anyMatch(mission -> mission.equals(productSource));
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
        MissionPlatform missionPlatform = getMissionPlatform(parameters);
        return Optional.ofNullable(SUPPORTED_MISSIONS.get(missionPlatform))
                .orElseThrow(() -> new IllegalArgumentException("Could not identify CreoDIAS Resto collection for mission: " + missionPlatform));
    }

    private MissionPlatform getMissionPlatform(SearchParameters parameters) {
        return MissionPlatform.builder()
                .mission(parameters.getValue("mission", "UNKNOWN"))
                .platform(parameters.getValue("platform", null))
                .build();
    }

    @Override
    protected SearchResults postProcess(SearchResults results) {
        results.getFeatures().forEach(f -> addFstepProperties(f, results.getParameters()));
        return results;
    }

    @SuppressWarnings("unchecked")
    private void addFstepProperties(Feature feature, SearchParameters parameters) {
        String collection = feature.getProperty("collection");
        String productSource = collection.toLowerCase();
        String productIdentifier = feature.getProperty("title");
        URI fstepUri = externalProductService.getUri(productSource, productIdentifier);

        // Shuffle the CreoDIAS properties into a sub-object for consistency across all search providers
        Map<String, Object> extraParams = new HashMap<>(feature.getProperties());
        feature.getProperties().clear();

        Set<Link> featureLinks = new HashSet<>();
        featureLinks.add(new Link(fstepUri.toASCIIString(), "fstep"));
        //when no services are available, the cast to map will fail.
        Long filesize;
        if (extraParams.get("services") instanceof List) {
        	filesize = 0L;
        }
        else {
        	filesize = Optional.ofNullable((Map<String, Map<String, Object>>) extraParams.get("services"))
                .map(services -> Optional.ofNullable(services.get("download")).map(dl -> ((Number) dl.get("size")).longValue()).orElse(0L))
                .orElse(0L);
        }
        // Required parameters for FstepFile ingestion
        feature.setProperty("productSource", productSource);
        feature.setProperty("productIdentifier", productIdentifier);
        feature.setProperty("fstepUrl", fstepUri);
        feature.setProperty("filesize", filesize);
        feature.setProperty("fstepUsable", true);

        // Set "interesting" parameters which clients might want in an easily-accessible form
        // Some are not present depending on the result type, so we have to safely traverse the dynamic properties map
        // These are added to extraParams so that the FstepFile/Resto schema is predictable
        Optional.ofNullable(extraParams.get("startDate"))
        .ifPresent(startDate -> extraParams.put("fstepStartTime", startDate));
        Optional.ofNullable(extraParams.get("completionDate"))
        .ifPresent(completionDate -> extraParams.put("fstepEndTime", completionDate));
        Optional.ofNullable(extraParams.get("cloudCover"))
                .ifPresent(cloudCoverage -> extraParams.put("fstepCloudCoverage", cloudCoverage));
        Optional.ofNullable(extraParams.get("orbitDirection"))
                .ifPresent(orbitDirection -> extraParams.put("fstepOrbitDirection", orbitDirection));
        Optional.ofNullable(extraParams.get("productType"))
                .ifPresent(productType -> extraParams.put("fstepProductType", productType));
        Optional.ofNullable(extraParams.get("published"))
        .ifPresent(published -> extraParams.put("fstepUpdated", published));
        feature.setProperty("extraParams", extraParams);

        Object quicklookURL = extraParams.get("thumbnail");
        if (quicklookURL != null) {
        		featureLinks.add(new Link(quicklookURL.toString(), "quicklook"));
        }
        /*TODO A cleaner approach than above would be to build an internal reference to the quicklook and avoid
         * providing a direct URL (which might not be available to the users), as below.  
         * 
        HttpUrl.Builder quicklookUrlBuilder = parameters.getRequestUrl().newBuilder();
        parameters.getRequestUrl().queryParameterNames().forEach(quicklookUrlBuilder::removeAllQueryParameters);
        quicklookUrlBuilder.addPathSegment("ql").addPathSegment(productSource).addPathSegment(productIdentifier);
        featureLinks.add(new Link(quicklookUrlBuilder.build().toString(), "quicklook"));
        */
        feature.setProperty("_links", featureLinks.stream().collect(Collectors.toMap(
                Link::getRel,
                l -> ImmutableMap.of("href", l.getHref())
        )));
    }
    
    @Override
    public boolean supportsDynamicParameter(String parameter) {
        return false;
    }
    
    @Override
    public List<Map<String, Object>> getDynamicParameterValues(String parameter){
        return Collections.<Map<String, Object>>emptyList();
    }

    @Override
    public String getDynamicParameterDefaultValue(String parameter){
        return StringUtils.EMPTY;
    }
}
