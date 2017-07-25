package com.cgi.eoss.fstep.server;

import com.cgi.eoss.fstep.api.ApiConfig;
import com.cgi.eoss.fstep.catalogue.CatalogueConfig;
import com.cgi.eoss.fstep.orchestrator.OrchestratorConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * <p>Application running the FS-TEP orchestrator and associated "master" services.</p>
 */
@Import({
        ApiConfig.class,
        CatalogueConfig.class,
        OrchestratorConfig.class
})
@SpringBootApplication(scanBasePackageClasses = FstepServerApplication.class)
public class FstepServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(FstepServerApplication.class, args);
    }

}
