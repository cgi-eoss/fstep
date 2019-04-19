package com.cgi.eoss.fstep.persistence.service;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.FstepFilesRelation;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.persistence.PersistenceConfig;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {PersistenceConfig.class})
@TestPropertySource("classpath:test-persistence.properties")
@Transactional
public class FstepFilesRelationDataServiceIT {
    @Autowired
    private FstepFileDataService fileDataService;
    
    @Autowired
    private FstepFilesRelationDataService dataService;
    
    @Autowired
    private UserDataService userService;

    @Test
    public void test() throws Exception {
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
        fstepFile2.setOwner(owner);

        FstepFile fstepFile3 = new FstepFile();
        fstepFile3.setUri(URI.create("fstep://fstepFile3"));
        fstepFile3.setRestoId(UUID.randomUUID());
        fstepFile3.setOwner(owner);
        
        fileDataService.save(ImmutableSet.of(fstepFile, fstepFile2, fstepFile3));

        //Save a relation between fstepFile and fstepFile2
        FstepFilesRelation relation = new FstepFilesRelation(fstepFile, fstepFile2, FstepFilesRelation.Type.VISUALIZATION_OF);
        dataService.save(relation);
        
        //Save a relation between fstepFile and fstepFile2
        FstepFilesRelation relation1 = new FstepFilesRelation(fstepFile, fstepFile3, FstepFilesRelation.Type.VISUALIZATION_OF);
        
        dataService.save(relation1);
        
        //Retrieve files associated with fstepFile
        Set<FstepFilesRelation> related = dataService.findBySourceFileAndType(fstepFile, FstepFilesRelation.Type.VISUALIZATION_OF);
        assertThat(related.stream().map(r -> r.getTargetFile()).collect(Collectors.toList()), is(ImmutableList.of(fstepFile2, fstepFile3)));
        
        //Remove the source file
        fstepFile = fileDataService.getByRestoId(fstepFile.getRestoId());
        fileDataService.delete(fstepFile);
        
        assertThat(fileDataService.getAll(), is(ImmutableList.of(fstepFile2, fstepFile3)));
        
        related = dataService.findBySourceFileAndType(fstepFile, FstepFilesRelation.Type.VISUALIZATION_OF);
        
        assertThat(related.size(), is(0));
    }

}