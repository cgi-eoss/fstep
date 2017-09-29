package com.cgi.eoss.fstep.persistence.dao;

import java.util.List;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.model.UserPreference;

public interface UserPreferenceDao extends FstepEntityDao<UserPreference> {

    List<UserPreference> findByOwner(User user);

    UserPreference findOneByNameAndOwner(String name, User user);

    List<UserPreference> findByTypeAndOwner(String type, User user);

}
