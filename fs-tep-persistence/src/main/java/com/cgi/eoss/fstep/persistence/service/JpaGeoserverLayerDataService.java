package com.cgi.eoss.fstep.persistence.service;

import static com.cgi.eoss.fstep.model.QGeoserverLayer.geoserverLayer;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.GeoserverLayer;
import com.cgi.eoss.fstep.persistence.dao.GeoserverLayerDao;
import com.cgi.eoss.fstep.persistence.dao.FstepEntityDao;
import com.querydsl.core.types.Predicate;

@Service
@Transactional(readOnly = true)
public class JpaGeoserverLayerDataService extends AbstractJpaDataService<GeoserverLayer> implements GeoserverLayerDataService {

    private final GeoserverLayerDao dao;

    @Autowired
    public JpaGeoserverLayerDataService(GeoserverLayerDao dao) {
        this.dao = dao;
    }

    @Override
    FstepEntityDao<GeoserverLayer> getDao() {
        return dao;
    }

	@Override
	Predicate getUniquePredicate(GeoserverLayer entity) {
	     return geoserverLayer.workspace.eq(entity.getWorkspace()).and(geoserverLayer.layer.eq(entity.getLayer()));
	}

	@Transactional
	@Override
	public void syncGeoserverLayers(FstepFile fstepFile) {
		Set<GeoserverLayer> fileLayers = fstepFile.getGeoserverLayers();
		Set<GeoserverLayer> syncedLayers = new HashSet<>();
		for (GeoserverLayer fileLayer : fileLayers) {
			GeoserverLayer syncedLayer = this.findOneByExample(fileLayer);
			if (syncedLayer == null) {
				syncedLayer = fileLayer;
			}
			syncedLayer.getFiles().add(fstepFile);
			syncedLayers.add(syncedLayer);
		}
		fstepFile.setGeoserverLayers(syncedLayers);
	}

	@Override
	@Transactional
	public GeoserverLayer refreshFull(GeoserverLayer layer) {
		layer = refresh(layer);
		layer.getFiles().size();
		return layer;
	}

}
