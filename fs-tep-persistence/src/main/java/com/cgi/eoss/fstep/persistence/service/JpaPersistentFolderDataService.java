package com.cgi.eoss.fstep.persistence.service;

import static com.cgi.eoss.fstep.model.QPersistentFolder.persistentFolder;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cgi.eoss.fstep.model.PersistentFolder;
import com.cgi.eoss.fstep.model.PersistentFolder.Status;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.persistence.dao.FstepEntityDao;
import com.cgi.eoss.fstep.persistence.dao.PersistentFolderDao;
import com.querydsl.core.types.Predicate;

@Service
@Transactional(readOnly = true)
public class JpaPersistentFolderDataService extends AbstractJpaDataService<PersistentFolder>
        implements PersistentFolderDataService {

    private final PersistentFolderDao dao;

    @Autowired
    public JpaPersistentFolderDataService(PersistentFolderDao persistentFolderDao) {
        this.dao = persistentFolderDao;
    }
    
    @Override
    FstepEntityDao<PersistentFolder> getDao() {
        return dao;
    }

    @Override
    Predicate getUniquePredicate(PersistentFolder entity) {
        return persistentFolder.name.eq(entity.getName())
                .and(persistentFolder.owner.eq(entity.getOwner()));
    }

	@Override
	public List<PersistentFolder> findByOwnerAndStatus(User user, Status status) {
		return dao.findByOwnerAndStatus(user, status);
	}
  
}
