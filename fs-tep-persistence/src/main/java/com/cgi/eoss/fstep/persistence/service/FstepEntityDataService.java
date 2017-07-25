package com.cgi.eoss.fstep.persistence.service;

import com.cgi.eoss.fstep.model.FstepEntity;

/**
 * A directory to store, load and delete FstepEntity objects. Provides data integrity and constraint checks
 * before passing to the DAO.
 *
 * @param <T> The data type to be provided.
 */
public interface FstepEntityDataService<T extends FstepEntity<T>> extends DataService<T, Long> {

}
