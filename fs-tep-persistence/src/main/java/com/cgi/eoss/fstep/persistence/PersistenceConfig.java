package com.cgi.eoss.fstep.persistence;

import com.cgi.eoss.fstep.model.FstepEntity;
import com.cgi.eoss.fstep.persistence.dao.FstepEntityDao;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@Import({
        PropertyPlaceholderAutoConfiguration.class,
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        FlywayAutoConfiguration.class,
})
@EnableJpaRepositories(basePackageClasses = FstepEntityDao.class,
        excludeFilters = {@ComponentScan.Filter(SpringJpaRepositoryIgnore.class)})
@EnableTransactionManagement
@EntityScan(basePackageClasses = FstepEntity.class)
@ComponentScan(basePackageClasses = PersistenceConfig.class)
public class PersistenceConfig {

}
