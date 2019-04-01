package com.cgi.eoss.fstep.docker;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * <p>Application to collect Docker events</p>
 */
@Import({
	AppConfig.class
})
@SpringBootApplication(scanBasePackageClasses = FstepDockerEventCollectorApplication.class)
public class FstepDockerEventCollectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(FstepDockerEventCollectorApplication.class, args);
    }

}
