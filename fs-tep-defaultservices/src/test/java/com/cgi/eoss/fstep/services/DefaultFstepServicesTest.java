package com.cgi.eoss.fstep.services;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import java.util.Set;
import org.junit.Test;
import com.cgi.eoss.fstep.model.FstepService;

public class DefaultFstepServicesTest {

    @Test
    public void getDefaultServices() throws Exception {
        Set<FstepService> defaultServices = DefaultFstepServices.getDefaultServices();
        assertThat(defaultServices.size(), is(3));
   }

}