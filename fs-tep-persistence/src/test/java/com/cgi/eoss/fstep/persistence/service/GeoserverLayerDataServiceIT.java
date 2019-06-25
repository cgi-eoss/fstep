package com.cgi.eoss.fstep.persistence.service;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import com.cgi.eoss.fstep.model.GeoserverLayer;
import com.cgi.eoss.fstep.model.GeoserverLayer.StoreType;
import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.persistence.PersistenceConfig;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import java.net.URI;
import java.util.UUID;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {PersistenceConfig.class})
@TestPropertySource("classpath:test-persistence.properties")
@Transactional
public class GeoserverLayerDataServiceIT {
    @Autowired
    private FstepFileDataService fileDataService;
    
    @Autowired
    private GeoserverLayerDataService geoserverLayerDataService;
    
    
    @Autowired
    private UserDataService userService;

    @Test
    public void testSaveGeoserverLayer() throws Exception {
        User owner = new User("owner-uid");
        User owner2 = new User("owner-uid2");
        userService.save(ImmutableSet.of(owner, owner2));

        FstepFile fstepFile = new FstepFile();
        fstepFile.setUri(URI.create("fstep://fstepFile"));
        fstepFile.setRestoId(UUID.randomUUID());
        fstepFile.setOwner(owner);
        
        FstepFile fstepFile2 = new FstepFile();
        fstepFile2.setUri(URI.create("fstep://fstepFile2"));
        fstepFile2.setRestoId(UUID.randomUUID());
        fstepFile2.setOwner(owner2);

        GeoserverLayer geoserverLayer = new GeoserverLayer(owner, "test", "test", StoreType.MOSAIC);
        fstepFile.getGeoserverLayers().add(geoserverLayer);
        geoserverLayerDataService.syncGeoserverLayers(fstepFile);
        fileDataService.save(ImmutableSet.of(fstepFile, fstepFile2));
        assertThat(geoserverLayerDataService.getAll().size(), is (1));
        GeoserverLayer layer = geoserverLayerDataService.getAll().get(0);
        assertThat(layer.getFiles().size(), is (1));
    }
    
    @Test
    public void testUpdateGeoserverLayer() throws Exception {
        User owner = new User("owner-uid");
        userService.save(ImmutableSet.of(owner));

        FstepFile fstepFile1 = new FstepFile();
        fstepFile1.setUri(URI.create("fstep://fstepFile"));
        fstepFile1.setRestoId(UUID.randomUUID());
        fstepFile1.setOwner(owner);
        
        GeoserverLayer geoserverLayer1 = new GeoserverLayer(owner, "test", "test", StoreType.MOSAIC);
        fstepFile1.getGeoserverLayers().add(geoserverLayer1);
        
        geoserverLayerDataService.syncGeoserverLayers(fstepFile1);
        fileDataService.save(ImmutableSet.of(fstepFile1));
        
        assertThat(geoserverLayerDataService.getAll().size(), is (1));
        GeoserverLayer layer = geoserverLayerDataService.getAll().get(0);
        layer = geoserverLayerDataService.refreshFull(layer);
        assertThat(layer.getFiles().size(), is (1));
       
        FstepFile fstepFile2 = new FstepFile();
        fstepFile2.setUri(URI.create("fstep://fstepFile2"));
        fstepFile2.setRestoId(UUID.randomUUID());
        fstepFile2.setOwner(owner);
        
        GeoserverLayer geoserverLayer2 = new GeoserverLayer(owner, "test", "test", StoreType.POSTGIS);
        fstepFile2.getGeoserverLayers().add(geoserverLayer2);
        geoserverLayerDataService.syncGeoserverLayers(fstepFile2);
        
        //fstepFile2.setGeoserverLayers(layers);
        fileDataService.save(ImmutableSet.of(fstepFile2));
        assertThat(geoserverLayerDataService.getAll().size(), is (1));
        layer = geoserverLayerDataService.getAll().get(0);
        layer = geoserverLayerDataService.refreshFull(layer);
        assertThat(layer.getFiles().size(), is (2));
    }
    
    @Test
    public void testDeleteGeoserverLayer() throws Exception {
        User owner = new User("owner-uid");
        userService.save(ImmutableSet.of(owner));

        FstepFile fstepFile1 = new FstepFile();
        fstepFile1.setUri(URI.create("fstep://fstepFile"));
        fstepFile1.setRestoId(UUID.randomUUID());
        fstepFile1.setOwner(owner);
        
        GeoserverLayer geoserverLayer1 = new GeoserverLayer(owner, "test", "test", StoreType.GEOTIFF);
        fstepFile1.getGeoserverLayers().add(geoserverLayer1);
        
        geoserverLayerDataService.syncGeoserverLayers(fstepFile1);
        fileDataService.save(ImmutableSet.of(fstepFile1));
        
        assertThat(geoserverLayerDataService.getAll().size(), is (1));
        GeoserverLayer layer = geoserverLayerDataService.getAll().get(0);
        layer = geoserverLayerDataService.refreshFull(layer);
        assertThat(layer.getFiles().size(), is (1));
       
        fileDataService.delete(fstepFile1);
        fstepFile1.getGeoserverLayers().forEach( l -> geoserverLayerDataService.delete(l));
        assertThat(geoserverLayerDataService.getAll().size(), is (0));
        
    }

}