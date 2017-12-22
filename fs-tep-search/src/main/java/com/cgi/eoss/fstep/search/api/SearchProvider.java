package com.cgi.eoss.fstep.search.api;

import com.google.common.collect.ComparisonChain;
import org.springframework.core.io.Resource;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface SearchProvider extends Comparable<SearchProvider> {
    int getPriority();

    SearchResults search(SearchParameters parameters) throws IOException;

    Map<String, String> getPagingParameters(SearchParameters parameters);

    Map<String, String> getQueryParameters(SearchParameters parameters);

    boolean supports(SearchParameters parameters);

    boolean supportsQuicklook(String productSource, String productIdentifier);

    Resource getQuicklook(String productSource, String productIdentifier) throws IOException;

    boolean supportsDynamicParameter(String parameter);
    
    List<Map<String, Object>> getDynamicParameterValues(String parameter);

    String getDynamicParameterDefaultValue(String parameter);
    
    @Override
    default int compareTo(SearchProvider o) {
        return ComparisonChain.start().compare(getPriority(), o.getPriority()).result();
    }

}