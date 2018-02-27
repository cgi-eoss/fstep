package com.cgi.eoss.fstep.model.converters;

import com.cgi.eoss.fstep.model.FstepServiceResources;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import lombok.extern.log4j.Log4j2;
import java.io.IOException;

@Converter
@Log4j2
public class FstepServiceResourcesYamlConverter implements AttributeConverter<FstepServiceResources, String> {

    private static final TypeReference<FstepServiceResources> FSTEP_SERVICE_RESOURCES = new TypeReference<FstepServiceResources>() { };

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

    public FstepServiceResourcesYamlConverter() {
    }

    @Override
    public String convertToDatabaseColumn(FstepServiceResources attribute) {
        return toYaml(attribute);
    }

    @Override
    public FstepServiceResources convertToEntityAttribute(String dbData) {
        return dbData!=null?fromYaml(dbData):null;
    }

    public static String toYaml(FstepServiceResources fstepServiceResources) {
        try {
            return MAPPER.writeValueAsString(fstepServiceResources);
        } catch (JsonProcessingException e) {
            LOG.error("Failed to convert FstepServiceResources to YAML string: {}", fstepServiceResources);
            throw new IllegalArgumentException("Could not convert FstepServiceResources to YAML string", e);
        }
    }

    public static FstepServiceResources fromYaml(String yaml) {
        try {
            return MAPPER.readValue(yaml, FSTEP_SERVICE_RESOURCES);
        } catch (IOException e) {
            LOG.error("Failed to convert YAML string to FstepServiceResources: {}", yaml);
            throw new IllegalArgumentException("Could not convert YAML string to FstepServiceResources", e);
        }
    }

}

