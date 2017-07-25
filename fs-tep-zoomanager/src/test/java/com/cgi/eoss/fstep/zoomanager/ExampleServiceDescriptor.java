package com.cgi.eoss.fstep.zoomanager;

import com.cgi.eoss.fstep.model.FstepServiceDescriptor;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.List;

public final class ExampleServiceDescriptor {
    private static final List<FstepServiceDescriptor.Parameter> INPUTS = ImmutableList.of(
            FstepServiceDescriptor.Parameter.builder()
                    .id("inputfile")
                    .title("Input File 1")
                    .description("The input data file")
                    .minOccurs(1)
                    .maxOccurs(1)
                    .data(FstepServiceDescriptor.Parameter.DataNodeType.LITERAL)
                    .defaultAttrs(ImmutableMap.<String, String>builder()
                            .put("dataType", "string")
                            .build())
                    .build()
    );
    private static final List<FstepServiceDescriptor.Parameter> OUTPUTS = ImmutableList.of(
            FstepServiceDescriptor.Parameter.builder()
                    .id("result")
                    .title("URL to service output")
                    .description("see title")
                    .data(FstepServiceDescriptor.Parameter.DataNodeType.LITERAL)
                    .defaultAttrs(ImmutableMap.<String, String>builder()
                            .put("dataType", "string").build())
                    .build()
    );

    private static final FstepServiceDescriptor EXAMPLE_SVC = FstepServiceDescriptor.builder()
            .id("TestService1")
            .title("Test Service for ZCFG Generation")
            .description("This service tests the FS-TEP automatic zcfg file generation")
            .version("1.0")
            .serviceProvider("fstep_service_wrapper")
            .serviceType("python")
            .storeSupported(false)
            .statusSupported(false)
            .dataInputs(INPUTS)
            .dataOutputs(OUTPUTS)
            .build();


    public static FstepServiceDescriptor getExampleSvc() {
        return EXAMPLE_SVC;
    }
}
