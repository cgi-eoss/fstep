package com.cgi.eoss.fstep.persistence.service;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.nio.CharBuffer;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.model.UserPreference;
import com.cgi.eoss.fstep.persistence.PersistenceConfig;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {PersistenceConfig.class})
@TestPropertySource("classpath:test-persistence.properties")
@Transactional
public class UserPreferenceDataServiceIT {
    @Autowired
    private UserPreferenceDataService dataService;
    
    @Autowired
    private UserDataService userService;
    
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    
    @Test
    public void test() throws Exception {
    	
    		User owner = new User("owner-uid");
        User owner2 = new User("owner-uid2");
        userService.save(ImmutableSet.of(owner, owner2));
        
        String name1 = "prefName";
        String name2 = "prefName2";
        
        
        UserPreference pref_name1_owner = new UserPreference(name1, "prefType", "prefValue");
        
        UserPreference pref2_name2_owner2 = new UserPreference(name2, "prefType", "prefValue");
       
        UserPreference pref3_name1_owner2 = new UserPreference(name1, "prefType", "prefValue");
        
        pref_name1_owner.setOwner(owner);
        pref2_name2_owner2.setOwner(owner2);
        pref3_name1_owner2.setOwner(owner2);
        
        dataService.save(ImmutableSet.of(pref_name1_owner, pref2_name2_owner2, pref3_name1_owner2));

        assertThat(dataService.getById(pref_name1_owner.getId()), is(pref_name1_owner));
        assertThat(dataService.getById(pref2_name2_owner2.getId()), is(pref2_name2_owner2));
        assertThat(dataService.getById(pref3_name1_owner2.getId()), is(pref3_name1_owner2));
        assertThat(dataService.getByIds(ImmutableSet.of(pref_name1_owner.getId(), pref2_name2_owner2.getId())), is(ImmutableList.of(pref_name1_owner, pref2_name2_owner2)));
        assertThat(dataService.getByNameAndOwner(name1, owner), is (pref_name1_owner));
        assertThat(dataService.getByNameAndOwner(name2, owner2), is (pref2_name2_owner2));
        assertThat(dataService.getByNameAndOwner(name1, owner2), is (pref3_name1_owner2));
        
        
        UserPreference pref_name1_owner_duplicate = new UserPreference(name1, "prefType", "prefValue");
        pref_name1_owner_duplicate.setOwner(owner);
        assertThat(dataService.isUniqueAndValid(pref_name1_owner_duplicate), is(false));
        
        UserPreference pref_name2_owner1 = new UserPreference(name2, "prefType", "prefValue");
        pref_name2_owner1.setOwner(owner);
        assertThat(dataService.isUniqueAndValid(pref_name2_owner1), is(true));
        
        assertThat(dataService.getByNameAndOwner(name1, owner), is (pref_name1_owner));
        
        assertThat(dataService.findByTypeAndOwner("prefType", owner), is (ImmutableList.of(pref_name1_owner)));
        
        UserPreference longString = new UserPreference("long", "longString", zeroesStringOfLength(40000));
        
        longString.setOwner(owner);
        
        dataService.save(longString);
    }
    
    @Test
    public void testPreferenceTooLong() {
    		UserPreference tooLongString = new UserPreference("tooLong", "longString", zeroesStringOfLength(100000));
    		User owner = new User("owner-uid");
    		userService.save(owner);
    		tooLongString.setOwner(owner);
    	 	exception.expect(DataIntegrityViolationException.class);
        dataService.save(tooLongString);
    }
    
    private String zeroesStringOfLength(int length) {
    	  return CharBuffer.allocate( length ).toString();
    	}

}