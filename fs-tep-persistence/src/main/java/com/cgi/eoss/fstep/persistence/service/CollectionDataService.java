package com.cgi.eoss.fstep.persistence.service;

import com.cgi.eoss.fstep.model.Collection;
import com.cgi.eoss.fstep.model.FstepFile.Type;
import com.cgi.eoss.fstep.model.User;
import java.util.List;
import java.util.Set;

public interface CollectionDataService extends
        FstepEntityDataService<Collection>,
        SearchableDataService<Collection> {

    Collection getByNameAndOwner(String name, User user);

    List<Collection> findByOwner(User user);

    Collection getByIdentifier(String collectionIdentifier);
    
    Set<Collection> findByFileType(Type fileType);
}
