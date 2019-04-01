package com.cgi.eoss.fstep.worker;

import com.cgi.eoss.fstep.worker.jobs.WorkerJob;
import com.cgi.eoss.fstep.worker.jobs.WorkerJobRepository;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableJpaRepositories(basePackageClasses = WorkerJobRepository.class,
entityManagerFactoryRef = "workerJobsEntityManager", 
transactionManagerRef = "workerJobsTransactionManager")
@EntityScan(basePackageClasses = WorkerJob.class)
public class WorkerJobsPersistenceConfig {
 
    @Bean
    @ConfigurationProperties("fstep.worker.jobs.datasource")
    public DataSourceProperties workerJobsDataSourceProperties() {
        return new DataSourceProperties();
    }
    
    @Bean
    public DataSource workerJobsDataSource() {
        return workerJobsDataSourceProperties().initializeDataSourceBuilder().build();
    }
    
    
    @Bean
    public LocalContainerEntityManagerFactoryBean workerJobsEntityManager() {
        HibernateJpaVendorAdapter jpaVendorAdapter = new HibernateJpaVendorAdapter();
        jpaVendorAdapter.setGenerateDdl(true);

        LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();

        factoryBean.setDataSource(workerJobsDataSource());
        factoryBean.setJpaVendorAdapter(jpaVendorAdapter);
        factoryBean.setPackagesToScan(WorkerJobsPersistenceConfig.class.getPackage().getName());

        return factoryBean;
    }
    
    @Bean
    public PlatformTransactionManager workerJobsTransactionManager() {
        JpaTransactionManager transactionManager
          = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(
          workerJobsEntityManager().getObject());
        return transactionManager;
    }

}
