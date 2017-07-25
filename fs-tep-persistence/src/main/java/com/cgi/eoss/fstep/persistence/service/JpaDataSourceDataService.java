package com.cgi.eoss.fstep.persistence.service;

import com.cgi.eoss.fstep.model.DataSource;
import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.persistence.dao.DataSourceDao;
import com.cgi.eoss.fstep.persistence.dao.FstepEntityDao;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.cgi.eoss.fstep.model.QDataSource.dataSource;

@Service
@Transactional(readOnly = true)
public class JpaDataSourceDataService extends AbstractJpaDataService<DataSource> implements DataSourceDataService {

    private final DataSourceDao dao;
    private final UserDataService userDataService;

    @Autowired
    public JpaDataSourceDataService(DataSourceDao dataSourceDao, UserDataService userDataService) {
        this.dao = dataSourceDao;
        this.userDataService = userDataService;
    }

    @Override
    FstepEntityDao<DataSource> getDao() {
        return dao;
    }

    @Override
    Predicate getUniquePredicate(DataSource entity) {
        return dataSource.name.eq(entity.getName());
    }

    @Override
    public List<DataSource> search(String term) {
        return dao.findByNameContainingIgnoreCase(term);
    }

    @Override
    public DataSource getByName(String name) {
        return dao.findOneByName(name);
    }

    @Override
    public List<DataSource> findByOwner(User user) {
        return dao.findByOwner(user);
    }

    @Transactional
    @Override
    public DataSource getForService(FstepService service) {
        return getOrCreate(service.getDataSourceName());
    }

    @Transactional
    @Override
    public DataSource getForExternalProduct(FstepFile fstepFile) {
        return getOrCreate(fstepFile.getUri().getScheme());
    }

    @Transactional
    @Override
    public DataSource getForRefData(FstepFile fstepFile) {
        return getOrCreate(fstepFile.getUri().getScheme());
    }

    private DataSource getOrCreate(String name) {
        return maybeGetByName(name).orElseGet(() -> save(new DataSource(name, userDataService.getDefaultUser())));
    }

    private Optional<DataSource> maybeGetByName(String name) {
        return Optional.ofNullable(dao.findOneByName(name));
    }

}
