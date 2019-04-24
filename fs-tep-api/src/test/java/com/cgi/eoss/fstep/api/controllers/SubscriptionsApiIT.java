package com.cgi.eoss.fstep.api.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.cgi.eoss.fstep.api.ApiConfig;
import com.cgi.eoss.fstep.api.ApiTestConfig;
import com.cgi.eoss.fstep.model.BillingScheme;
import com.cgi.eoss.fstep.model.CostQuotation;
import com.cgi.eoss.fstep.model.CostQuotation.Recurrence;
import com.cgi.eoss.fstep.model.Role;
import com.cgi.eoss.fstep.model.Subscription;
import com.cgi.eoss.fstep.model.Subscription.Status;
import com.cgi.eoss.fstep.model.SubscriptionPlan;
import com.cgi.eoss.fstep.model.UsageType;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.persistence.service.SubscriptionDataService;
import com.cgi.eoss.fstep.persistence.service.SubscriptionPlanDataService;
import com.cgi.eoss.fstep.persistence.service.UserDataService;
import com.google.common.collect.ImmutableSet;
import com.jayway.jsonpath.JsonPath;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = { ApiConfig.class, ApiTestConfig.class })
@AutoConfigureMockMvc
@TestPropertySource("classpath:test-api.properties")
@Transactional
public class SubscriptionsApiIT {

	@Autowired
	private SubscriptionPlanDataService subscriptionPlanDataService;

	@Autowired
	private SubscriptionDataService dataService;

	@Autowired
	private UserDataService userDataService;
	
	@Autowired
	private MockMvc mockMvc;

	private User fstepUser;
	private User fstepUser2;
	private User fstepAdmin;

	private SubscriptionPlan plan;

	private SubscriptionPlan plan2;

	private static final JsonPath SELF_HREF_JSONPATH = JsonPath.compile("$._links.self.href");
    

	@Before
	public void setUp() {
		fstepUser = new User("fstep-user");
		fstepUser.setRole(Role.USER);
		fstepUser2 = new User("fstep-user-2");
		fstepUser2.setRole(Role.USER);
		fstepAdmin = new User("fstep-admin");
		fstepAdmin.setRole(Role.ADMIN);

		userDataService.save(ImmutableSet.of(fstepUser, fstepUser2, fstepAdmin));

		plan = new SubscriptionPlan("Storage Basic", UsageType.FILES_STORAGE_MB, 1000, 1, 99, BillingScheme.UNIT,
				new CostQuotation(5, Recurrence.MONTHLY));

		plan2 = new SubscriptionPlan("Storage Medium", UsageType.FILES_STORAGE_MB, 1000, 100, 1000, BillingScheme.UNIT,
				new CostQuotation(4, Recurrence.MONTHLY));

		subscriptionPlanDataService.save(ImmutableSet.of(plan, plan2));
		Subscription subscription = new Subscription(fstepUser, plan, 50, OffsetDateTime.now());
		Subscription subscription2 = new Subscription(fstepAdmin, plan, 50, OffsetDateTime.now());
		dataService.save(ImmutableSet.of(subscription, subscription2));
	}

	@After
	public void tearDown() {
		dataService.deleteAll();
	}

	@Test
	public void testFindAll() throws Exception {
		mockMvc.perform(get("/api/subscriptions/").header("REMOTE_USER", fstepUser.getName()))
				.andExpect(status().isOk()).andExpect(jsonPath("$._embedded.subscriptions").isArray())
				.andExpect(jsonPath("$._embedded.subscriptions.length()").value(1));
	}

	@Test
	public void testCreate() throws Exception {
		String body = "{\"owner\":\"" + userUri(fstepUser2) + "\", \"subscriptionPlan\":\"" + subscriptionPlanUri(plan)
				+ "\", \"quantity\":\"10\" }";
		mockMvc.perform(post("/api/subscriptions/").header("REMOTE_USER", fstepUser2.getName()).content(body))
				.andExpect(status().isCreated());
	}

	@Test(expected = Exception.class)
	public void testCreateForExistingPlanFails() throws Exception {
		String body = "{\"owner\":\"" + userUri(fstepUser) + "\", \"subscriptionPlan\":\"" + subscriptionPlanUri(plan)
				+ "\", \"quantity\":\"10\" }";
		mockMvc.perform(post("/api/subscriptions/").header("REMOTE_USER", fstepUser.getName()).content(body));
	}

	@Test(expected = Exception.class)
	public void testCreateForSameUsageTypeFails() throws Exception {
		String body = "{\"owner\":\"" + userUri(fstepUser) + "\", \"subscriptionPlan\":\"" + subscriptionPlanUri(plan2)
				+ "\", \"quantity\":\"10\" }";
		mockMvc.perform(post("/api/subscriptions/").header("REMOTE_USER", fstepUser.getName()).content(body));
	}

	@Test()
	public void testChange() throws Exception {
		String body = "{\"owner\":\"" + userUri(fstepUser2) + "\", \"subscriptionPlan\":\"" + subscriptionPlanUri(plan2)
				+ "\", \"quantity\":\"10\" }";
		mockMvc.perform(post("/api/subscriptions/").header("REMOTE_USER", fstepUser2.getName()).content(body))
				.andExpect(status().isCreated());
		long id = dataService.findByOwnerAndSubscriptionPlanUsageTypeAndStatusIsNot(fstepUser2,
				UsageType.FILES_STORAGE_MB, Status.TERMINATED).getId();
		String change = "{\"owner\":\"" + userUri(fstepUser2) + "\", \"subscriptionPlan\":\""
				+ subscriptionPlanUri(plan2) + "\", \"quantity\":\"20\" }";
		mockMvc.perform(patch("/api/subscriptions/" + id).header("REMOTE_USER", fstepUser2.getName()).content(change))
		.andExpect(status().isNoContent());
	}

	private String userUri(User user) throws Exception {
		String jsonResult = mockMvc
				.perform(get("/api/users/" + user.getId()).header("REMOTE_USER", fstepAdmin.getName())).andReturn()
				.getResponse().getContentAsString();
		return SELF_HREF_JSONPATH.read(jsonResult);
	}

	private String subscriptionPlanUri(SubscriptionPlan subscriptionPlan) throws Exception {
		String jsonResult = mockMvc.perform(
				get("/api/subscriptionPlans/" + subscriptionPlan.getId()).header("REMOTE_USER", fstepAdmin.getName()))
				.andReturn().getResponse().getContentAsString();
		return SELF_HREF_JSONPATH.read(jsonResult);
	}

}
