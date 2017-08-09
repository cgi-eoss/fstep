package com.cgi.eoss.fstep.search.fstep;

import com.cgi.eoss.fstep.catalogue.resto.RestoService;
import com.cgi.eoss.fstep.search.api.RepoType;
import com.cgi.eoss.fstep.search.api.SearchParameters;
import com.cgi.eoss.fstep.search.api.SearchResults;
import com.cgi.eoss.fstep.search.resto.RestoSearchProvider;
import com.cgi.eoss.fstep.search.resto.RestoSearchResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;

import java.util.HashMap;
import java.util.Map;

@Log4j2
public class FstepSearchProvider extends RestoSearchProvider {

    private final RestoService restoService;

    public FstepSearchProvider(FstepSearchProperties searchProperties, OkHttpClient httpClient, ObjectMapper objectMapper, RestoService restoService) {
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
        this.restoService = restoService;
    }

    @Override
    public Map<String, String> getQueryParameters(SearchParameters parameters) {
        Map<String, String> queryParameters = new HashMap<>();

        parameters.getValue("keyword").ifPresent(s -> queryParameters.put("productIdentifier", "%" + s + "%"));
        parameters.getValue("geometry").ifPresent(s -> queryParameters.put("geometry", s));
        parameters.getValue("owner").ifPresent(s -> queryParameters.put("owner", s));

        return queryParameters;
    }

    @Override
    public boolean supports(RepoType repoType, SearchParameters parameters) {
        return repoType == RepoType.FSTEP_PRODUCTS || repoType == RepoType.REF_DATA;
    }

    @Override
    protected Map<String, SearchResults.Link> getLinks(HttpUrl requestUrl, SearchResults.Page page, RestoSearchResult restoResult) {
        Map<String, SearchResults.Link> links = new HashMap<>();

        links.putAll(getPagingLinks(page, requestUrl));

        return links;
    }

    @Override
    protected String getCollection(SearchParameters parameters) {
        switch (parameters.getRepo()) {
            case REF_DATA:
                return restoService.getReferenceDataCollection();
            case FSTEP_PRODUCTS:
                return restoService.getOutputProductsCollection();
            default:
                throw new IllegalArgumentException("Could not identify Resto collection for repo type: " + parameters.getRepo());
        }
    }

    @Override
    protected SearchResults postProcess(SearchResults results) {
        return results;
    }

}
