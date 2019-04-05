package com.cgi.eoss.fstep.persistence.service;

import java.util.List;

import com.cgi.eoss.fstep.model.PersistentFolder;
import com.cgi.eoss.fstep.model.PersistentFolder.Status;
import com.cgi.eoss.fstep.model.User;

public interface PersistentFolderDataService extends FstepEntityDataService<PersistentFolder> {

	List<PersistentFolder> findByOwnerAndStatus(User owner, Status status);
  
}
