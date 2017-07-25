package com.cgi.eoss.fstep.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(WorkerConfig.class)
public class FstepWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(FstepWorkerApplication.class, args);
    }

}
