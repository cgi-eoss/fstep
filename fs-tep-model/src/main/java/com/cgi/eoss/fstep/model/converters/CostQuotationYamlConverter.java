package com.cgi.eoss.fstep.model.converters;

import java.io.IOException;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.cgi.eoss.fstep.model.CostQuotation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import lombok.extern.log4j.Log4j2;

@Converter
@Log4j2
public class CostQuotationYamlConverter implements AttributeConverter<CostQuotation, String> {

    private static final TypeReference<CostQuotation> COST_QUOTATION = new TypeReference<CostQuotation>() { };

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

    public CostQuotationYamlConverter() {
    }

    @Override
    public String convertToDatabaseColumn(CostQuotation costQuotation) {
        return toYaml(costQuotation);
    }

    @Override
    public CostQuotation convertToEntityAttribute(String dbData) {
        return dbData!=null?fromYaml(dbData):null;
    }

    public static String toYaml(CostQuotation costQuotation) {
        try {
            return MAPPER.writeValueAsString(costQuotation);
        } catch (JsonProcessingException e) {
            LOG.error("Failed to convert CostQuotation to YAML string: {}", costQuotation);
            throw new IllegalArgumentException("Could not convert CostQuotation to YAML string", e);
        }
    }

    public static CostQuotation fromYaml(String yaml) {
        try {
            return MAPPER.readValue(yaml, COST_QUOTATION);
        } catch (IOException e) {
            LOG.error("Failed to convert YAML string to CostQuotation: {}", yaml);
            throw new IllegalArgumentException("Could not convert YAML string to CostQuotation", e);
        }
    }

}

