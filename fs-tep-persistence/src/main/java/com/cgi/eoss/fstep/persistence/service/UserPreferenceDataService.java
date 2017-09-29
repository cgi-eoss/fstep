package com.cgi.eoss.fstep.persistence.service;

import java.util.List;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.model.UserPreference;

public interface UserPreferenceDataService extends FstepEntityDataService<UserPreference> {

    UserPreference getByNameAndOwner(String name, User user);

    List<UserPreference> findByTypeAndOwner(String type, User user);

}
