package com.cgi.eoss.fstep.quotas;

import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.cgi.eoss.fstep.persistence.PersistenceConfig;
import com.cgi.eoss.fstep.rpc.InProcessRpcConfig;

@Configuration
@Import({ InProcessRpcConfig.class, PropertyPlaceholderAutoConfiguration.class, PersistenceConfig.class, })
@ComponentScan(basePackageClasses = QuotasConfig.class)
public class QuotasConfig {

}
