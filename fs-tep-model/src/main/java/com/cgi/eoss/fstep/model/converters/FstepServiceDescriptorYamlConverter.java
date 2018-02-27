package com.cgi.eoss.fstep.model.converters;

import com.cgi.eoss.fstep.model.FstepServiceDescriptor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.log4j.Log4j2;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.io.IOException;

@Converter
@Log4j2
public class FstepServiceDescriptorYamlConverter implements AttributeConverter<FstepServiceDescriptor, String> {

    private static final TypeReference<FstepServiceDescriptor> FSTEP_SERVICE_DESCRIPTOR = new TypeReference<FstepServiceDescriptor>() { };

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

    public FstepServiceDescriptorYamlConverter() {
    }

    @Override
    public String convertToDatabaseColumn(FstepServiceDescriptor attribute) {
        return toYaml(attribute);
    }

    @Override
    public FstepServiceDescriptor convertToEntityAttribute(String dbData) {
        return fromYaml(dbData);
    }

    public static String toYaml(FstepServiceDescriptor fstepServiceDescriptor) {
        try {
            return MAPPER.writeValueAsString(fstepServiceDescriptor);
        } catch (JsonProcessingException e) {
            LOG.error("Failed to convert FstepServiceDescriptor to YAML string: {}", fstepServiceDescriptor);
            throw new IllegalArgumentException("Could not convert FstepServiceDescriptor to YAML string", e);
        }
    }

    public static FstepServiceDescriptor fromYaml(String yaml) {
        try {
            return MAPPER.readValue(yaml, FSTEP_SERVICE_DESCRIPTOR);
        } catch (IOException e) {
            LOG.error("Failed to convert YAML string to FstepServiceDescriptor: {}", yaml);
            throw new IllegalArgumentException("Could not convert YAML string to FstepServiceDescriptor", e);
        }
    }

}

