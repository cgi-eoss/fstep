package com.cgi.eoss.fstep.model.converters;

import com.cgi.eoss.fstep.model.FstepServiceDockerBuildInfo;
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
public class FstepServiceDockerBuildInfoYamlConverter implements AttributeConverter<FstepServiceDockerBuildInfo, String> {

    private static final TypeReference FSTEP_SERVICE_DOCKER_BUILD_INFO = new TypeReference<FstepServiceDockerBuildInfo>() { };

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

    public FstepServiceDockerBuildInfoYamlConverter() {
    }

    @Override
    public String convertToDatabaseColumn(FstepServiceDockerBuildInfo attribute) {
        return toYaml(attribute);
    }

    @Override
    public FstepServiceDockerBuildInfo convertToEntityAttribute(String dbData) {
        return dbData!=null?fromYaml(dbData):null;
    }

    public static String toYaml(FstepServiceDockerBuildInfo fstepServiceDockerBuildInfo) {
        try {
            return MAPPER.writeValueAsString(fstepServiceDockerBuildInfo);
        } catch (JsonProcessingException e) {
            LOG.error("Failed to convert FstepServiceDockerBuildInfo to YAML string: {}", fstepServiceDockerBuildInfo);
            throw new IllegalArgumentException("Could not convert FstepServiceDockerBuildInfo to YAML string", e);
        }
    }

    public static FstepServiceDockerBuildInfo fromYaml(String yaml) {
        try {
            return MAPPER.readValue(yaml, FSTEP_SERVICE_DOCKER_BUILD_INFO);
        } catch (IOException e) {
            LOG.error("Failed to convert YAML string to FstepServiceDockerBuildInfo: {}", yaml);
            throw new IllegalArgumentException("Could not convert YAML string to FstepServiceDockerBuildInfo", e);
        }
    }

}

