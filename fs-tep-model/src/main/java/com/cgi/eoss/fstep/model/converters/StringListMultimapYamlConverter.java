package com.cgi.eoss.fstep.model.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.collect.ListMultimap;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import lombok.extern.log4j.Log4j2;
import java.io.IOException;

@Converter
@Log4j2
public class StringListMultimapYamlConverter implements AttributeConverter<ListMultimap<String, String>, String> {

    private static final TypeReference<ListMultimap<String,String>> STRING_LISTMULTIMAP = new TypeReference<ListMultimap<String,String>>() { };

    private final ObjectMapper mapper;

    public StringListMultimapYamlConverter() {
        mapper = new ObjectMapper(new YAMLFactory()).registerModule(new GuavaModule());
    }

    @Override
    public String convertToDatabaseColumn(ListMultimap<String, String> attribute) {
        try {
            return mapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            LOG.error("Failed to convert ListMultimap to YAML string: {}", attribute);
            throw new IllegalArgumentException("Could not convert ListMultimap to YAML string", e);
        }
    }

    @Override
    public ListMultimap<String, String> convertToEntityAttribute(String dbData) {
        try {
            return mapper.readValue(dbData, STRING_LISTMULTIMAP);
        } catch (IOException e) {
            LOG.error("Failed to convert YAML string to ListMultimap: {}", dbData);
            throw new IllegalArgumentException("Could not convert YAML string to ListMultimap", e);
        }
    }
}

