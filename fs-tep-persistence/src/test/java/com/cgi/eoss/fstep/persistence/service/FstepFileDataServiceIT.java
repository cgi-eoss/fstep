package com.cgi.eoss.fstep.persistence.service;

import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.persistence.PersistenceConfig;
import com.google.common.collect.ImmutableList;
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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {PersistenceConfig.class})
@TestPropertySource("classpath:test-persistence.properties")
@Transactional
public class FstepFileDataServiceIT {
    @Autowired
    private FstepFileDataService dataService;
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

        dataService.save(ImmutableSet.of(fstepFile, fstepFile2));

        assertThat(dataService.getAll(), is(ImmutableList.of(fstepFile, fstepFile2)));
        assertThat(dataService.getById(fstepFile.getId()), is(fstepFile));
        assertThat(dataService.getByIds(ImmutableSet.of(fstepFile.getId())), is(ImmutableList.of(fstepFile)));
        assertThat(dataService.isUniqueAndValid(new FstepFile(URI.create("fstep://fstepFile"), UUID.randomUUID())), is(false));
        assertThat(dataService.isUniqueAndValid(new FstepFile(URI.create("fstep://newUri"), fstepFile.getRestoId())), is(false));
        assertThat(dataService.isUniqueAndValid(new FstepFile(URI.create("fstep://newUri"), UUID.randomUUID())), is(true));

        assertThat(dataService.findByOwner(owner), is(ImmutableList.of(fstepFile, fstepFile2)));
        assertThat(dataService.findByOwner(owner2), is(ImmutableList.of()));
        assertThat(dataService.getByRestoId(fstepFile.getRestoId()), is(fstepFile));
        assertThat(dataService.getByRestoId(fstepFile2.getRestoId()), is(fstepFile2));
    }

}