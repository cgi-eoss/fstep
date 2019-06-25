package com.cgi.eoss.fstep.persistence.service;

import static com.cgi.eoss.fstep.model.QFstepFile.fstepFile;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.Job;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.persistence.dao.FstepEntityDao;
import com.cgi.eoss.fstep.persistence.dao.FstepFileDao;
import com.querydsl.core.types.Predicate;

@Service
@Transactional(readOnly = true)
public class JpaFstepFileDataService extends AbstractJpaDataService<FstepFile> implements FstepFileDataService {

    private final FstepFileDao dao;
	private GeoserverLayerDataService geoserverLayerDataService;

    @Autowired
    public JpaFstepFileDataService(FstepFileDao fstepFileDao, GeoserverLayerDataService geoserverLayerDataService) {
        this.dao = fstepFileDao;
        this.geoserverLayerDataService = geoserverLayerDataService;
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
    
    @Override
    public Long sumFilesizeByOwner(User user) {
    	Long sum = dao.sumFilesizeByOwner(user);
    	if (sum != null) {
    		return sum;
    	}
    	return 0L;
    }

    @Override
    @Transactional
    public FstepFile syncGeoserverLayersAndSave(FstepFile fstepFile) {
        geoserverLayerDataService.syncGeoserverLayers(fstepFile);
        return this.save(fstepFile);
    }
    
    @Override
    @Transactional
    public void delete(FstepFile entity) {
    	for(Job j: entity.getJobs()) {
    		j.getOutputFiles().remove(entity);
    	}
    	entity.getJobs().clear();
    	super.delete(entity);
    }
}
