package com.cgi.eoss.fstep.catalogue;

import org.apache.commons.text.StrSubstitutor;

import java.net.URI;
import java.util.Map;

public enum CatalogueUri {
    REFERENCE_DATA("fstep://refData/${ownerId}/${filename}"),
    OUTPUT_PRODUCT("fstep://outputProduct/${jobId}/${filename}"),
    DATABASKET("fstep://databasket/${id}");

    private final String internalUriPattern;

    CatalogueUri(String internalUriPattern) {
        this.internalUriPattern = internalUriPattern;
    }

    public URI build(Map<String, String> values) {
        return URI.create(StrSubstitutor.replace(internalUriPattern, values));
    }
}
