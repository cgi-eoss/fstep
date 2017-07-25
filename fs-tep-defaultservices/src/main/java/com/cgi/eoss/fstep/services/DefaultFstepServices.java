package com.cgi.eoss.fstep.services;

import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.FstepServiceContextFile;
import com.cgi.eoss.fstep.model.FstepServiceDescriptor;
import com.cgi.eoss.fstep.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteStreams;
import lombok.extern.log4j.Log4j2;
import org.jooq.lambda.Unchecked;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>Access to the default FS-TEP service collection as Java objects.</p>
 * <p>The services are read from classpath resources baked in at compile-time, and may be used to install or restore the
 * default service set during runtime.</p>
 */
@Component
@Log4j2
public class DefaultFstepServices {

    private static final Map<String, FstepService.Type> DEFAULT_SERVICES = ImmutableMap.<String, FstepService.Type>builder()
            .put("ForestChangeS2", FstepService.Type.PROCESSOR)
            .put("LandCoverS1", FstepService.Type.PROCESSOR)
            .put("LandCoverS2", FstepService.Type.PROCESSOR)
            .put("S1Biomass", FstepService.Type.PROCESSOR)
            .put("VegetationIndices", FstepService.Type.PROCESSOR)
            .put("Monteverdi", FstepService.Type.APPLICATION)
            .put("QGIS", FstepService.Type.APPLICATION)
            .put("SNAP", FstepService.Type.APPLICATION)
            .build();

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    public static Set<FstepService> getDefaultServices() {
        return DEFAULT_SERVICES.keySet().stream().map(DefaultFstepServices::importDefaultService).collect(Collectors.toSet());
    }

    private static FstepService importDefaultService(String serviceId) {
        try {
            FstepService service = new FstepService(serviceId, User.DEFAULT, "fstep/" + serviceId.toLowerCase());
            service.setLicence(FstepService.Licence.OPEN);
            service.setStatus(FstepService.Status.AVAILABLE);
            service.setServiceDescriptor(getServiceDescriptor(service));
            service.setDescription(service.getServiceDescriptor().getDescription());
            service.setType(DEFAULT_SERVICES.get(serviceId));
            service.setContextFiles(getServiceContextFiles(service));
            return service;
        } catch (IOException e) {
            throw new RuntimeException("Could not load default FS-TEP Service " + serviceId, e);
        }
    }

    private static FstepServiceDescriptor getServiceDescriptor(FstepService service) throws IOException {
        try (Reader reader = new InputStreamReader(DefaultFstepServices.class.getResourceAsStream("/" + service.getName() + ".yaml"))) {
            return YAML_MAPPER.readValue(reader, FstepServiceDescriptor.class);
        }
    }

    private static Set<FstepServiceContextFile> getServiceContextFiles(FstepService service) throws IOException {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(DefaultFstepServices.class.getClassLoader());
        Resource baseDir = resolver.getResource("classpath:/" + service.getName());
        Set<Resource> resources = ImmutableSet.copyOf(resolver.getResources("classpath:/" + service.getName() + "/**/*"));

        return resources.stream()
                .filter(Unchecked.predicate(r -> !r.getURI().toString().endsWith("/")))
                .map(Unchecked.function(r -> FstepServiceContextFile.builder()
                        .service(service)
                        .filename(getRelativeFilename(r, baseDir))
                        .executable(r.getFilename().endsWith(".sh"))
                        .content(new String(ByteStreams.toByteArray(r.getInputStream())))
                        .build()
                ))
                .collect(Collectors.toSet());
    }

    private static String getRelativeFilename(Resource resource, Resource baseDir) throws IOException {
        return resource.getURI().toString().replaceFirst(baseDir.getURI().toString() + "/", "");
    }

}
