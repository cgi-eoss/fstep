package com.cgi.eoss.fstep.search.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Log4j2
public class SearchFacade {

    private final List<SearchProvider> searchProviders;
    private final ObjectMapper yamlMapper;
    private final String parametersSchemaFile;

    public SearchFacade(Collection<SearchProvider> searchProviders, String parametersSchemaFile) {
        this.searchProviders = ImmutableList.sortedCopyOf(searchProviders);
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.parametersSchemaFile = parametersSchemaFile;
    }

    public SearchResults search(SearchParameters parameters) throws IOException {
        SearchProvider provider = getProvider(parameters);
        return provider.search(parameters);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getParametersSchema(boolean resolveAll) throws IOException {
        InputStream parametersFile = Strings.isNullOrEmpty(parametersSchemaFile)
                ? getClass().getResourceAsStream("parameters.yaml")
                : Files.newInputStream(Paths.get(parametersSchemaFile));

        Map<String, Object> parameterSchema = yamlMapper.readValue(ByteStreams.toByteArray(parametersFile), new TypeReference<Map<String, Object>>() { });
        //TODO fill with dynamic parameters from providers
        if (resolveAll) {
	        for (String parameter: parameterSchema.keySet()) {
	            Map<String, Object> parameterObj = (Map<String, Object>) parameterSchema.get(parameter);
	            if (parameterObj.containsKey("type") && "dynamic".equals(parameterObj.get("type"))) {
	                parameterObj.put("type", "select");
	                Map<String, Object> allowedValues = (Map<String, Object>) parameterObj.get("allowed");
	                List<Map<String, Object>> values = new ArrayList<Map<String, Object>>();
	                for (SearchProvider sp: searchProviders) {
	                    if (sp.supportsDynamicParameter(parameter)) {
	                        List<Map<String, Object>> additionalValues = sp.getDynamicParameterValues(parameter);
	                        values.addAll(additionalValues);
	                        String defaultValue = sp.getDynamicParameterDefaultValue(parameter);
	                        parameterObj.put ("defaultValue", defaultValue);
	                    }
	                }
	                allowedValues.put("values", values);
	                
	            }
	        }
        }
        return parameterSchema;
    }
    
    @SuppressWarnings("unchecked")
    public Object getDynamicParameter(String parameterName) throws IOException {
    	InputStream parametersFile = Strings.isNullOrEmpty(parametersSchemaFile)
                ? getClass().getResourceAsStream("parameters.yaml")
                : Files.newInputStream(Paths.get(parametersSchemaFile));
    	Map<String, Object> parameterSchema = yamlMapper.readValue(ByteStreams.toByteArray(parametersFile), new TypeReference<Map<String, Object>>() { });
        Map<String, Object> parameterObj = (Map<String, Object>) parameterSchema.get(parameterName);
        if (parameterObj.containsKey("type") && "dynamic".equals(parameterObj.get("type"))) {
            parameterObj.put("type", "select");
            Map<String, Object> allowedValues = (Map<String, Object>) parameterObj.get("allowed");
            List<Map<String, Object>> values = new ArrayList<Map<String, Object>>();
            for (SearchProvider sp: searchProviders) {
                if (sp.supportsDynamicParameter(parameterName)) {
                    List<Map<String, Object>> additionalValues = sp.getDynamicParameterValues(parameterName);
                    values.addAll(additionalValues);
                    String defaultValue = sp.getDynamicParameterDefaultValue(parameterName);
                    parameterObj.put ("defaultValue", defaultValue);
                }
            }
            allowedValues.put("values", values);
            
        }
        return parameterObj;
    }
    

    private SearchProvider getProvider(SearchParameters parameters) {
        return searchProviders.stream()
                .filter(sp -> sp.supports(parameters))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No search providers found for parameters: " + parameters));
    }

    public Resource getQuicklookResource(String productSource, String productIdentifier) throws IOException {
        SearchProvider provider = getQuicklooksProvider(productSource, productIdentifier);
        return provider.getQuicklook(productSource, productIdentifier);
    }

    private SearchProvider getQuicklooksProvider(String productSource, String productIdentifier) {
        return searchProviders.stream()
                .filter(sp -> sp.supportsQuicklook(productSource, productIdentifier))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No quicklook providers found for product: " + productIdentifier));
    }
}