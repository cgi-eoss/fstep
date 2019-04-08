package com.cgi.eoss.fstep.persistence.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.FstepFilesCumulativeUsageRecord;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.persistence.PersistenceConfig;
import com.google.common.collect.ImmutableSet;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {PersistenceConfig.class})
@TestPropertySource("classpath:test-persistence.properties")
@Transactional
public class FstepFilesCumulativeUsageRecordDataServiceIT {
    @Autowired
    private UserDataService userService;

    @Autowired
    private FstepFilesCumulativeUsageRecordDataService dataService;

    @Autowired
    private FstepFileDataService fstepFileDataService;
    
    @Rule
    public final ExpectedException exception = ExpectedException.none();


    @Test
    public void testDateRanges() throws Exception {

        User owner = new User("owner-uid");
        userService.save(ImmutableSet.of(owner));
        LocalDate record = LocalDate.parse("2019-02-05", DateTimeFormatter.ISO_LOCAL_DATE);
        LocalDate record2 = LocalDate.parse("2019-02-05", DateTimeFormatter.ISO_LOCAL_DATE);
        LocalDate record3 = LocalDate.parse("2019-02-18", DateTimeFormatter.ISO_LOCAL_DATE);
        LocalDate record4 = LocalDate.parse("2019-02-20", DateTimeFormatter.ISO_LOCAL_DATE);
        LocalDate record5 = LocalDate.parse("2019-02-25", DateTimeFormatter.ISO_LOCAL_DATE);
        
        dataService.save(new FstepFilesCumulativeUsageRecord(owner, record, FstepFile.Type.OUTPUT_PRODUCT, 5000L));
        dataService.save(new FstepFilesCumulativeUsageRecord(owner, record, null, 5000L));
        dataService.save(new FstepFilesCumulativeUsageRecord(owner, record2, FstepFile.Type.OUTPUT_PRODUCT, 5200L));
        dataService.save(new FstepFilesCumulativeUsageRecord(owner, record2, null, 5200L));
        dataService.save(new FstepFilesCumulativeUsageRecord(owner, record3, FstepFile.Type.REFERENCE_DATA, 4900L));
        dataService.save(new FstepFilesCumulativeUsageRecord(owner, record3, null, 4900L));
        dataService.save(new FstepFilesCumulativeUsageRecord(owner, record4, FstepFile.Type.OUTPUT_PRODUCT, 5300L));
        dataService.save(new FstepFilesCumulativeUsageRecord(owner, record4, null, 5300L));
        dataService.save(new FstepFilesCumulativeUsageRecord(owner, record5, FstepFile.Type.OUTPUT_PRODUCT, 5500L));
        dataService.save(new FstepFilesCumulativeUsageRecord(owner, record5, null, 5500L));
        
        LocalDate start = LocalDate.parse("2019-02-03", DateTimeFormatter.ISO_LOCAL_DATE);
        LocalDate end = LocalDate.parse("2019-02-20", DateTimeFormatter.ISO_LOCAL_DATE);
        
        FstepFilesCumulativeUsageRecord nonExistingUsageRecord = dataService.findTopByOwnerAndFileTypeIsNullAndRecordDateLessThanEqualOrderByRecordDateDesc(owner, start);
        assertThat(nonExistingUsageRecord, is(nullValue()));
        
        List<FstepFilesCumulativeUsageRecord> records = dataService.findByOwnerAndFileTypeIsNullAndRecordDateBetween(owner, start, end);
        assertThat(records.size(), is(3));
        
        List<FstepFilesCumulativeUsageRecord> outputRecords = dataService.findByOwnerAndFileTypeAndRecordDateBetween(owner, FstepFile.Type.OUTPUT_PRODUCT, start, end);
        assertThat(outputRecords.size(), is(2));
        
        List<FstepFilesCumulativeUsageRecord> referenceRecords = dataService.findByOwnerAndFileTypeAndRecordDateBetween(owner, FstepFile.Type.REFERENCE_DATA, start, end);
        assertThat(referenceRecords.size(), is(1));
        
    }
    
    @Test
    public void testUpdateUsageRecords() throws Exception {

        User owner = new User("owner-uid");
        userService.save(ImmutableSet.of(owner));
        FstepFile file = new FstepFile(URI.create("file"), UUID.randomUUID());
        file.setOwner(owner);
        file.setFilesize(5000L);
        file.setType(FstepFile.Type.OUTPUT_PRODUCT);
        fstepFileDataService.save(file);
        dataService.updateUsageRecords(file);
        
        FstepFile file2 = new FstepFile(URI.create("file2"), UUID.randomUUID());
        file2.setFilesize(10000L);
        file2.setType(FstepFile.Type.OUTPUT_PRODUCT);
        file2.setOwner(owner);
        fstepFileDataService.save(file2);
        dataService.updateUsageRecords(file2);
        
        FstepFile file3 = new FstepFile(URI.create("file3"), UUID.randomUUID());
        file3.setFilesize(20000L);
        file3.setType(FstepFile.Type.REFERENCE_DATA);
        file3.setOwner(owner);
        fstepFileDataService.save(file3);
        dataService.updateUsageRecords(file3);
        
        assertThat(dataService.getAll().size(), is(3));
        
        FstepFile file4 = new FstepFile(URI.create("file4"), UUID.randomUUID());
        file4.setFilesize(7000L);
        file4.setOwner(owner);
        fstepFileDataService.save(file4);
        dataService.updateUsageRecords(file4);
        
        FstepFile file5 = new FstepFile(URI.create("file5"), UUID.randomUUID());
        file5.setOwner(owner);
        fstepFileDataService.save(file5);
        dataService.updateUsageRecords(file5);
        
        assertThat(dataService.getAll().size(), is(3));
        
        FstepFilesCumulativeUsageRecord outputUsageRecord = dataService.findTopByOwnerAndFileTypeAndRecordDateLessThanEqualOrderByRecordDateDesc(owner, FstepFile.Type.OUTPUT_PRODUCT, LocalDate.now());
        assertThat(outputUsageRecord.getCumulativeSize(), is(15000L));

        FstepFilesCumulativeUsageRecord referenceUsageRecord = dataService.findTopByOwnerAndFileTypeAndRecordDateLessThanEqualOrderByRecordDateDesc(owner, FstepFile.Type.REFERENCE_DATA, LocalDate.now());
        assertThat(referenceUsageRecord.getCumulativeSize(), is(20000L));

        FstepFilesCumulativeUsageRecord overallUsageRecord = dataService.findTopByOwnerAndFileTypeIsNullAndRecordDateLessThanEqualOrderByRecordDateDesc(owner, LocalDate.now());
        assertThat(overallUsageRecord.getCumulativeSize(), is(42000L));

    }

}
