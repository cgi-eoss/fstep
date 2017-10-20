package com.cgi.eoss.fstep.api.controllers;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import org.geojson.Feature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.cgi.eoss.fstep.catalogue.CatalogueService;
import com.cgi.eoss.fstep.model.Databasket;
import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.persistence.service.DatabasketDataService;
import com.cgi.eoss.fstep.search.api.SearchResults;
import com.cgi.eoss.fstep.search.api.SearchResults.Page;
import lombok.extern.log4j.Log4j2;

@RestController
@BasePathAwareController
@RequestMapping("/databaskets")
@Log4j2
public class DataBasketsApiExtension {

    private SearchApi searchApi;
    private CatalogueService catalogueService;
    private DatabasketDataService databasketDataService;

    private int maxItemsDatabasketsFromSearch;

    @Autowired
    public DataBasketsApiExtension(SearchApi searchApi, CatalogueService catalogueService, DatabasketDataService databasketDataService,
            @Value("${fstep.api.maxItemsDatabasketsFromSearch:500}") int maxItemsDatabasketsFromSearch) {
        this.searchApi = searchApi;
        this.catalogueService = catalogueService;
        this.databasketDataService = databasketDataService;
        this.maxItemsDatabasketsFromSearch = maxItemsDatabasketsFromSearch;
    }


    @PostMapping("/{databasketId}/fromSearch")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#databasket, 'write')")
    ResponseEntity fromSearch(@ModelAttribute("databasketId") Databasket databasket, HttpServletRequest request) {
        //TODO this should interact with the search facade directly, instead of the search API. 
        try {
            LOG.debug("Populating databasket {} with search results", databasket.getId());
            SearchResults searchResults = searchApi.search(request);
            Page resultPage = searchResults.getPage();
            long totalElements = resultPage.getTotalElements();
            if (totalElements > maxItemsDatabasketsFromSearch) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(String.format("Too many search results to be added. Allowed: %s - requested %s",
                                maxItemsDatabasketsFromSearch, totalElements));
            }
            long pageNumber = resultPage.getNumber();
            long totalPages = resultPage.getTotalPages();
            addSearchResultsToDatabasket(databasket, searchResults);
            for (long i = pageNumber; i < totalPages; i++) {
                TreeMap<String, String[]> additionalParameters = new TreeMap<String, String[]>();
                additionalParameters.put("page", new String[] {Long.toString(i + 1)});
                RequestWithAdditionalParameters requestWithAdditionalParameters =
                        new RequestWithAdditionalParameters(request, additionalParameters);
                searchResults = searchApi.search(requestWithAdditionalParameters);
                addSearchResultsToDatabasket(databasket, searchResults);
            }
            databasketDataService.save(databasket);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(String.format("Search results added to databasket %s - %s", databasket.getId(), databasket.getName()));
        } catch (IOException e) {
            LOG.error("Unable to add search results to databasket {} Error: {}", databasket.getId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    String.format("Unable to add search results to databasket %s. Error: " + e.getMessage(), databasket.getId()));

        }
    }

    private void addSearchResultsToDatabasket(Databasket databasket, SearchResults searchResults) {
        List<Feature> features = searchResults.getFeatures();
        for (Feature feature : features) {
            Feature newFile = new Feature();
            newFile.setGeometry(feature.getGeometry());
            Map<String, Object> links = feature.getProperty("_links");
            Map<String, Object> fstepLink = (Map<String, Object>) links.get("fstep");
            String fstepLinkHref = (String) fstepLink.get("href");
            newFile.setProperty("productSource", feature.getProperty("productSource"));
            newFile.setProperty("productIdentifier", feature.getProperty("productIdentifier"));
            newFile.setProperty("originalUrl", fstepLinkHref);
            newFile.setProperty("extraParams", feature.getProperty("extraParams"));
            FstepFile fstepFile = catalogueService.indexExternalProduct(newFile);
            LOG.debug("Adding fstepFile {} to databasket {}", newFile.getId(), databasket.getId());
            databasket.getFiles().add(fstepFile);
        }
    }


    private class RequestWithAdditionalParameters extends HttpServletRequestWrapper {
        private Map<String, String[]> additionalParameters;
        private Map<String, String[]> allParameters;

        public RequestWithAdditionalParameters(final HttpServletRequest request, final Map<String, String[]> additionalParams) {
            super(request);
            additionalParameters = new TreeMap<String, String[]>();
            additionalParameters.putAll(additionalParams);
        }

        @Override
        public String getParameter(final String name) {
            String[] strings = getParameterMap().get(name);
            if (strings != null) {
                return strings[0];
            }
            return super.getParameter(name);
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            if (allParameters == null) {
                allParameters = new TreeMap<String, String[]>();
                allParameters.putAll(super.getParameterMap());
                allParameters.putAll(additionalParameters);
            }
            return Collections.unmodifiableMap(allParameters);
        }

        @Override
        public Enumeration<String> getParameterNames() {
            return Collections.enumeration(getParameterMap().keySet());
        }

        @Override
        public String[] getParameterValues(final String name) {
            return getParameterMap().get(name);
        }
    }

}
