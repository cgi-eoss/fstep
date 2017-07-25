package com.cgi.eoss.fstep.persistence.dao;

import com.cgi.eoss.fstep.model.Databasket;
import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.User;

import java.util.List;

public interface DatabasketDao extends FstepEntityDao<Databasket> {
    Databasket findOneByNameAndOwner(String name, User user);

    List<Databasket> findByNameContainingIgnoreCase(String term);

    List<Databasket> findByFilesContaining(FstepFile file);

    List<Databasket> findByOwner(User user);
}
