package com.cgi.eoss.fstep.persistence.dao;

import com.cgi.eoss.fstep.model.DataSource;
import com.cgi.eoss.fstep.model.User;

import java.util.List;

public interface DataSourceDao extends FstepEntityDao<DataSource> {
    List<DataSource> findByNameContainingIgnoreCase(String term);
    DataSource findOneByName(String name);
    List<DataSource> findByOwner(User user);
}
