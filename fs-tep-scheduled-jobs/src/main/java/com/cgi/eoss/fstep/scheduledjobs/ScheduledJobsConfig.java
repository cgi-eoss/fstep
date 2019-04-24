package com.cgi.eoss.fstep.scheduledjobs;


import com.cgi.eoss.fstep.persistence.PersistenceConfig;
import org.quartz.Scheduler;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import javax.sql.DataSource;

/**
 * <p>Spring configuration for the fstep Scheduled Jobs component.</p>
 */
@Configuration
@Import({
    PersistenceConfig.class
})
@ComponentScan(basePackageClasses = ScheduledJobsConfig.class)
public class ScheduledJobsConfig {

    @Bean
    @DependsOn("dataSource")
    public Scheduler scheduler(ApplicationContext applicationContext, DataSource datasource, PlatformTransactionManager transactionManager) throws Exception {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setTransactionManager(transactionManager);
        factory.setDataSource(datasource);
        Properties quartzProperties = quartzProperties().entrySet().stream()
                        .map(e -> new AbstractMap.SimpleEntry<String, Object>("org.quartz" + "." + e.getKey(), e.getValue()))
                        .flatMap(this::flattenToQuartzProperties)
                        .collect(Properties::new,
                                (properties, entry) -> properties.put(entry.getKey(), entry.getValue()),
                                (properties, properties2) -> properties2.forEach(properties::put));
        AutowiringSpringBeanJobFactory jobFactory = new AutowiringSpringBeanJobFactory();
        jobFactory.setApplicationContext(applicationContext);
        factory.setJobFactory(jobFactory);
        factory.setQuartzProperties(quartzProperties);
        factory.afterPropertiesSet();
        Scheduler scheduler = factory.getScheduler();
        scheduler.start();
        return scheduler;
    }
    
    private Stream<Map.Entry<String, String>> flattenToQuartzProperties(Map.Entry<String, Object> e) {
        if (e.getValue() instanceof Map) {
            return ((Map<?, ?>) e.getValue()).entrySet().stream()
                    .map(e1 -> new AbstractMap.SimpleEntry<String, Object>(e.getKey() + "." + e1.getKey(), e1.getValue()))
                    .flatMap(this::flattenToQuartzProperties);
        }
        return Stream.of(new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue().toString()));
    }
    
    @Bean
    @ConfigurationProperties(prefix="org.quartz")
    public Map<String, Object> quartzProperties() {
        return new HashMap<>();
    }

}
