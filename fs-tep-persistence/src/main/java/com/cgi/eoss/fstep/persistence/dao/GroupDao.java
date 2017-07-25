package com.cgi.eoss.fstep.persistence.dao;

import com.cgi.eoss.fstep.model.Group;
import com.cgi.eoss.fstep.model.User;

import java.util.List;

public interface GroupDao extends FstepEntityDao<Group> {
    Group findOneByName(String name);

    List<Group> findByNameContainingIgnoreCase(String term);

    List<Group> findByMembersContaining(User member);

    List<Group> findByOwner(User user);
}
