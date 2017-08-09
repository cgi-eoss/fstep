package com.cgi.eoss.fstep.search.ipt;

import com.cgi.eoss.fstep.search.api.RepoType;
import com.cgi.eoss.fstep.search.api.SearchParameters;
import com.cgi.eoss.fstep.search.api.SearchResults;
import com.cgi.eoss.fstep.search.resto.RestoSearchProvider;
import com.cgi.eoss.fstep.search.resto.RestoSearchResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import lombok.extern.log4j.Log4j2;
import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Log4j2
public class IptSearchProvider extends RestoSearchProvider {

    public IptSearchProvider(IptSearchProperties searchProperties, OkHttpClient httpClient, ObjectMapper objectMapper) {
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
    }

    @Override
    public Map<String, String> getQueryParameters(SearchParameters parameters) {
        Map<String, String> queryParameters = new HashMap<>();

        parameters.getValue("semantic").ifPresent(s -> queryParameters.put("q", s));
        parameters.getValue("productIdentifier").ifPresent(s -> queryParameters.put("productIdentifier", "%" + s + "%"));
        parameters.getValue("geometry").ifPresent(s -> queryParameters.put("geometry", s));

        return queryParameters;
    }

    @Override
    public boolean supports(RepoType repoType, SearchParameters parameters) {
        return repoType == RepoType.SATELLITE;
    }

    @Override
    protected Map<String, SearchResults.Link> getLinks(HttpUrl requestUrl, SearchResults.Page page, RestoSearchResult restoResult) {
        Map<String, SearchResults.Link> links = new HashMap<>();

        links.putAll(getPagingLinks(page, requestUrl));

        return links;
    }

    @Override
    protected String getCollection(SearchParameters parameters) {
        return parameters.getValue("collection").orElse("");
    }

    @Override
    protected SearchResults postProcess(SearchResults results) {
        // Attach generated FS-TEP service-compatible URI and quicklook links
        results.getFeatures().parallelStream()
                .forEach(feature -> {
                    List<Map<String, String>> links = feature.getProperty("links");

                    if (!Strings.isNullOrEmpty(feature.getProperty("thumbnail"))) {
                        links.add(ImmutableMap.of(
                                "rel", "fstepQuicklook",
                                "type", "image/*",
                                "title", "Product thumbnail",
                                "href", feature.getProperty("thumbnail")
                        ));
                    }
                });

        return results;
    }

}
