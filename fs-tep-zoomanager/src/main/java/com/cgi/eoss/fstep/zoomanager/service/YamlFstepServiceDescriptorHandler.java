package com.cgi.eoss.fstep.zoomanager.service;

import com.cgi.eoss.fstep.model.FstepServiceDescriptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * <p>{@link FstepServiceDescriptorHandler} implementation to produce and consume YAML-format FS-TEP service descriptor
 * files.</p>
 */
@Component
public class YamlFstepServiceDescriptorHandler implements FstepServiceDescriptorHandler {

    private final ObjectMapper mapper;

    public YamlFstepServiceDescriptorHandler() {
        this.mapper = new ObjectMapper(new YAMLFactory());
    }

    @Override
    public FstepServiceDescriptor readFile(Path file) {
        try (Reader reader = Files.newBufferedReader(file)) {
            return mapper.readValue(reader, FstepServiceDescriptor.class);
        } catch (IOException e) {
            throw new WpsDescriptorIoException("Could not read yaml service descriptor " + file, e);
        }
    }

    @Override
    public FstepServiceDescriptor read(InputStream stream) {
        try (Reader reader = new InputStreamReader(stream)) {
            return mapper.readValue(reader, FstepServiceDescriptor.class);
        } catch (IOException e) {
            throw new WpsDescriptorIoException("Could not read yaml service descriptor from " + stream, e);
        }
    }

    @Override
    public void writeFile(FstepServiceDescriptor svc, Path file) {
        try (Writer writer = Files.newBufferedWriter(file)) {
            mapper.writeValue(writer, svc);
        } catch (IOException e) {
            throw new WpsDescriptorIoException("Could not write yaml service descriptor " + file, e);
        }
    }

    @Override
    public void write(FstepServiceDescriptor svc, OutputStream stream) {
        try (Writer writer = new OutputStreamWriter(stream)) {
            mapper.writeValue(writer, svc);
        } catch (IOException e) {
            throw new WpsDescriptorIoException("Could not write yaml service descriptor " + stream, e);
        }
    }

}
