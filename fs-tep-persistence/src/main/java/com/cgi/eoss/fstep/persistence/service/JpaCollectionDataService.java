package com.cgi.eoss.fstep.persistence.service;

import static com.cgi.eoss.fstep.model.QCollection.collection;
import com.cgi.eoss.fstep.model.Collection;
import com.cgi.eoss.fstep.model.FstepFile.Type;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.persistence.dao.CollectionDao;
import com.cgi.eoss.fstep.persistence.dao.FstepEntityDao;
import com.querydsl.core.types.Predicate;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@Transactional(readOnly = true)
public class JpaCollectionDataService extends AbstractJpaDataService<Collection> implements CollectionDataService {

    private final CollectionDao dao;

    @Autowired
    public JpaCollectionDataService(CollectionDao collectionDao) {
        this.dao = collectionDao;
    }

    @Override
    FstepEntityDao<Collection> getDao() {
        return dao;
    }

    @Override
    Predicate getUniquePredicate(Collection entity) {
        return collection.name.eq(entity.getName()).and(collection.owner.eq(entity.getOwner()));
    }

    @Override
    public List<Collection> search(String term) {
        return dao.findByNameContainingIgnoreCase(term);
    }

    @Override
    public Collection getByNameAndOwner(String name, User user) {
        return dao.findOneByNameAndOwner(name, user);
    }
    
    @Override
    public Collection getByIdentifier(String collectionIdentifier) {
        return dao.findOneByIdentifier(collectionIdentifier);
    }

    @Override
    public List<Collection> findByOwner(User user) {
        return dao.findByOwner(user);
    }

    @Override
    public Collection refreshFull(Collection collection) {
    	collection = refresh(collection);
    	collection.getFstepFiles().size();
    	return collection;
    }

	@Override
	public Set<Collection> findByFileType(Type fileType) {
		return dao.findByFileType(fileType);
	}
}
