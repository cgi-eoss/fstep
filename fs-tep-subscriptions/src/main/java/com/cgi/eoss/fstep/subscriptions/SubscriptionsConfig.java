package com.cgi.eoss.fstep.subscriptions;

import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.cgi.eoss.fstep.costing.CostingConfig;
import com.cgi.eoss.fstep.persistence.PersistenceConfig;
import com.cgi.eoss.fstep.quotas.QuotasConfig;
import com.cgi.eoss.fstep.scheduledjobs.ScheduledJobsConfig;

@Configuration
@Import({
        PropertyPlaceholderAutoConfiguration.class,
        CostingConfig.class,
        PersistenceConfig.class,
        QuotasConfig.class,
        ScheduledJobsConfig.class
})
@ComponentScan(basePackageClasses = SubscriptionsConfig.class)
public class SubscriptionsConfig {

 

}
