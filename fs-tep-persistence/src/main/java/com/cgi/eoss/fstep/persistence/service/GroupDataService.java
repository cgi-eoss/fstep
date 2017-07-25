package com.cgi.eoss.fstep.persistence.service;

import com.cgi.eoss.fstep.model.Group;
import com.cgi.eoss.fstep.model.User;

import java.util.List;

public interface GroupDataService extends
        FstepEntityDataService<Group>,
        SearchableDataService<Group> {
    Group getByName(String name);

    List<Group> findGroupMemberships(User user);

    List<Group> findByOwner(User user);
}
