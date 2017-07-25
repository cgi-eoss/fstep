package com.cgi.eoss.fstep.persistence.service;

import com.cgi.eoss.fstep.model.Databasket;
import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.User;

import java.util.List;

public interface DatabasketDataService extends
        FstepEntityDataService<Databasket>,
        SearchableDataService<Databasket> {
    Databasket getByNameAndOwner(String name, User user);

    List<Databasket> findByFile(FstepFile file);

    List<Databasket> findByOwner(User user);
}
