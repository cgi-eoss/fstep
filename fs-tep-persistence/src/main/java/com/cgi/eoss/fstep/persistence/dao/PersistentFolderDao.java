package com.cgi.eoss.fstep.persistence.dao;

import java.util.List;

import com.cgi.eoss.fstep.model.PersistentFolder;
import com.cgi.eoss.fstep.model.PersistentFolder.Status;
import com.cgi.eoss.fstep.model.User;

public interface PersistentFolderDao extends FstepEntityDao<PersistentFolder> {

	List<PersistentFolder> findByOwnerAndStatus(User user, Status status);

}
