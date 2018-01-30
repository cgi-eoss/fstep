package com.cgi.eoss.fstep.persistence.service;

import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.FstepServiceContextFile;
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

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {PersistenceConfig.class})
@TestPropertySource("classpath:test-persistence.properties")
@Transactional
public class ServiceFileDataServiceIT {
    @Autowired
    private ServiceFileDataService dataService;
    @Autowired
    private ServiceDataService serviceDataService;
    @Autowired
    private UserDataService userService;

    @Test
    public void test() throws Exception {
        User owner = new User("owner-uid");
        User owner2 = new User("owner-uid2");
        userService.save(ImmutableSet.of(owner, owner2));

        FstepService svc = new FstepService();
        svc.setName("Test Service");
        svc.setOwner(owner);
        svc.setDockerTag("dockerTag");
        serviceDataService.save(svc);

        byte[] fileBytes = Files.readAllBytes(Paths.get(getClass().getResource("/testService/Dockerfile").toURI()));
        FstepServiceContextFile serviceFile = new FstepServiceContextFile();
        serviceFile.setService(svc);
        serviceFile.setFilename("Dockerfile");
        serviceFile.setContent(new String(fileBytes));
        dataService.save(serviceFile);

        assertThat(dataService.getAll(), is(ImmutableList.of(serviceFile)));
        assertThat(dataService.getById(serviceFile.getId()), is(serviceFile));
        assertThat(dataService.getByIds(ImmutableSet.of(serviceFile.getId())), is(ImmutableList.of(serviceFile)));
        assertThat(dataService.isUniqueAndValid(new FstepServiceContextFile(svc, "Dockerfile")), is(false));
        assertThat(dataService.isUniqueAndValid(new FstepServiceContextFile(svc, "Dockerfile2")), is(true));

        assertThat(dataService.findByService(svc), is(ImmutableList.of(serviceFile)));

        // Verify text file recovery
        assertThat(dataService.getById(serviceFile.getId()).getContent().getBytes(), is(fileBytes));
    }
    
    @Test
    public void testServiceFingerprint() throws Exception {
        User owner = new User("owner-uid");
        User owner2 = new User("owner-uid2");
        userService.save(ImmutableSet.of(owner, owner2));

        FstepService svc = new FstepService();
        svc.setName("Test Service");
        svc.setOwner(owner);
        svc.setDockerTag("dockerTag");
        
        FstepService svc2 = new FstepService();
        svc2.setName("Test Service 2");
        svc2.setOwner(owner);
        svc2.setDockerTag("dockerTag");
        serviceDataService.save(svc);
        serviceDataService.save(svc2);
        
        String serviceFingerPrint = serviceDataService.computeServiceFingerprint(svc);
        
        assertThat(serviceFingerPrint, is(notNullValue()));
        
        byte[] fileBytes = Files.readAllBytes(Paths.get(getClass().getResource("/testService/Dockerfile").toURI()));
        FstepServiceContextFile serviceFile = new FstepServiceContextFile();
        serviceFile.setService(svc);
        serviceFile.setFilename("Dockerfile");
        serviceFile.setContent(new String(fileBytes));
        dataService.save(serviceFile);
        serviceDataService.save(svc);
        
        String newServiceFingerPrint = serviceDataService.computeServiceFingerprint(svc);
        assertThat(serviceFingerPrint, is(not(newServiceFingerPrint)));
        
        serviceFile.setService(svc2);
        dataService.save(serviceFile);
        
        serviceDataService.save(svc);
        
        String shouldBeRestoredServiceFingerPrint = serviceDataService.computeServiceFingerprint(svc);
        
        assertThat(serviceFingerPrint, is((shouldBeRestoredServiceFingerPrint)));
        
        
  }

}