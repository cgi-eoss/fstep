package com.cgi.eoss.fstep.search.clms.model;

import java.util.List;

import org.geojson.Feature;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CLMSResult {
    @JsonProperty("entry")
    private List<CLMSProduct> products;
    
    @JsonProperty("totalResults")
    private Integer totalResults;

    @JsonProperty("itemsPerPage")
    private Integer itemsPerPage;
    
}
