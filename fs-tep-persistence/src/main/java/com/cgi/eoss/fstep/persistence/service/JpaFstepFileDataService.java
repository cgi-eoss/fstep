package com.cgi.eoss.fstep.persistence.service;

import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.persistence.dao.FstepEntityDao;
import com.cgi.eoss.fstep.persistence.dao.FstepFileDao;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static com.cgi.eoss.fstep.model.QFstepFile.fstepFile;

@Service
@Transactional(readOnly = true)
public class JpaFstepFileDataService extends AbstractJpaDataService<FstepFile> implements FstepFileDataService {

    private final FstepFileDao dao;

    @Autowired
    public JpaFstepFileDataService(FstepFileDao fstepFileDao) {
        this.dao = fstepFileDao;
    }

    @Override
    FstepEntityDao<FstepFile> getDao() {
        return dao;
    }

    @Override
    Predicate getUniquePredicate(FstepFile entity) {
        return fstepFile.uri.eq(entity.getUri()).or(fstepFile.restoId.eq(entity.getRestoId()));
    }

    @Override
    public FstepFile getByUri(URI uri) {
        return dao.findOneByUri(uri);
    }

    @Override
    public FstepFile getByUri(String uri) {
        return getByUri(URI.create(uri));
    }

    @Override
    public FstepFile getByRestoId(UUID uuid) {
        return dao.findOneByRestoId(uuid);
    }

    @Override
    public List<FstepFile> findByOwner(User user) {
        return dao.findByOwner(user);
    }

    @Override
    public List<FstepFile> getByType(FstepFile.Type type) {
        return dao.findByType(type);
    }

}
