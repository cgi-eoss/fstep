package com.cgi.eoss.fstep.services;

import com.cgi.eoss.fstep.model.FstepService;
import org.junit.Test;

import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class DefaultFstepServicesTest {

    @Test
    public void getDefaultServices() throws Exception {
        Set<FstepService> defaultServices = DefaultFstepServices.getDefaultServices();
        assertThat(defaultServices.size(), is(8));

        FstepService landCoverS2 = defaultServices.stream().filter(s -> s.getName().equals("LandCoverS2")).findFirst().get();
        assertThat(landCoverS2.getServiceDescriptor().getDataInputs().size(), is(6));
        assertThat(landCoverS2.getContextFiles().size(), is(7));
        assertThat(landCoverS2.getContextFiles().stream().anyMatch(f -> f.getFilename().equals("Dockerfile") && !f.isExecutable()), is(true));
        assertThat(landCoverS2.getContextFiles().stream().anyMatch(f -> f.getFilename().equals("workflow.sh") && f.isExecutable()), is(true));
    }

}