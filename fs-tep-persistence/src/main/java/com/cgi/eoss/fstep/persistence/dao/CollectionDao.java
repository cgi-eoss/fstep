package com.cgi.eoss.fstep.persistence.dao;

import com.cgi.eoss.fstep.model.Collection;
import com.cgi.eoss.fstep.model.User;
import java.util.List;

public interface CollectionDao extends FstepEntityDao<Collection> {
    Collection findOneByNameAndOwner(String name, User user);

    List<Collection> findByNameContainingIgnoreCase(String term);

    List<Collection> findByOwner(User user);
    
    Collection findOneByIdentifier(String identifier);
}
