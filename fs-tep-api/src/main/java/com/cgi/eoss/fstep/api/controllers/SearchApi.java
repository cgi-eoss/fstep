package com.cgi.eoss.fstep.api.controllers;

import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.persistence.service.FstepFileDataService;
import com.cgi.eoss.fstep.search.api.SearchFacade;
import com.cgi.eoss.fstep.search.api.SearchParameters;
import com.cgi.eoss.fstep.search.api.SearchResults;
import com.cgi.eoss.fstep.security.FstepSecurityService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;

/**
 * <p>Functionality for accessing the FS-TEP unifying search facade.</p>
 */
@RestController
@BasePathAwareController
@RequestMapping("/search")
@Log4j2
public class SearchApi {

    private final SearchFacade searchFacade;
    private final ObjectMapper objectMapper;
    private final FstepFileDataService fstepFileDataService;
    private final FstepSecurityService securityService;

    @Autowired
    public SearchApi(SearchFacade searchFacade, ObjectMapper objectMapper, FstepFileDataService fstepFileDataService, FstepSecurityService securityService) {
        // Handle single-/multi-value parameters
        this.objectMapper = objectMapper.copy()
                .configure(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED, true)
                .configure(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS, true)
                .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        this.searchFacade = searchFacade;
        this.fstepFileDataService = fstepFileDataService;
        this.securityService = securityService;
    }

    @GetMapping
    public SearchResults searchResults(HttpServletRequest request) throws IOException {
        // Do a two-way serialize/deserialize to convert the clumsy Map<String, String[]> to a nice SearchParameters object
        SearchParameters parameters = objectMapper.readValue(objectMapper.writeValueAsString(request.getParameterMap()), SearchParameters.class);
        parameters.setRequestUrl(HttpUrl.parse(request.getRequestURL().toString()).newBuilder().query(request.getQueryString()).build());
        SearchResults results = searchFacade.search(parameters);

        // Add visibility info, if the result can be matched to an FstepFile
        results.getFeatures().forEach(f -> {
            // Default to usable
            boolean fstepUsable = true;
            URI fstepUri = null;
            try {
                FstepFile fstepFile = fstepFileDataService.getByRestoId(UUID.fromString(f.getId()));
                if (fstepFile != null) {
                    fstepUsable = securityService.isReadableByCurrentUser(FstepFile.class, fstepFile.getId());
                    fstepUri = fstepFile.getUri();
                } else {
                    LOG.debug("No FstepFile found for search result with ID: {}", f.getId());
                }
            } catch (Exception e) {
                LOG.debug("Could not check visibility of search result with ID: {}", f.getId(), e);
            }
            f.getProperties().put("fstepUsable", fstepUsable);
            f.getProperties().put("fstepUri", fstepUri);
        });
        return results;
    }

}
