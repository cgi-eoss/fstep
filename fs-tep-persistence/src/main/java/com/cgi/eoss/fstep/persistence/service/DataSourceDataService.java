package com.cgi.eoss.fstep.persistence.service;

import com.cgi.eoss.fstep.model.DataSource;
import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.User;

import java.util.List;

public interface DataSourceDataService extends
        FstepEntityDataService<DataSource>,
        SearchableDataService<DataSource> {
    DataSource getByName(String name);

    List<DataSource> findByOwner(User user);

    DataSource getForService(FstepService service);

    DataSource getForExternalProduct(FstepFile fstepFile);

    DataSource getForRefData(FstepFile fstepFile);
}
