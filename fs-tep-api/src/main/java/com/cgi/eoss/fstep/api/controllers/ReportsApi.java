package com.cgi.eoss.fstep.api.controllers;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cgi.eoss.fstep.model.Job;
import com.cgi.eoss.fstep.model.JobProcessing;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.model.WalletTransaction;
import com.cgi.eoss.fstep.model.csv.CsvJob;
import com.cgi.eoss.fstep.model.csv.CsvJob.CsvJobBuilder;
import com.cgi.eoss.fstep.persistence.service.JobDataService;
import com.cgi.eoss.fstep.persistence.service.JobProcessingDataService;
import com.cgi.eoss.fstep.persistence.service.WalletTransactionDataService;
import com.cgi.eoss.fstep.security.FstepSecurityService;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@RestController
@BasePathAwareController
@RequestMapping("/reports")
public class ReportsApi {

	private final FstepSecurityService fstepSecurityService;
	private final JobDataService jobDataService;
	private final JobProcessingDataService jobProcessingDataService;
	private final WalletTransactionDataService walletTransactionDataService;
	private CsvSchema jobSchema;
	private CsvMapper mapper;
	@Autowired
	public ReportsApi(FstepSecurityService fstepSecurityService, JobDataService jobDataService, JobProcessingDataService jobProcessingDataService, WalletTransactionDataService walletTransactionDataService) {
		this.fstepSecurityService = fstepSecurityService;
		this.jobDataService = jobDataService;
		this.jobProcessingDataService = jobProcessingDataService;
		this.walletTransactionDataService = walletTransactionDataService;
		jobSchema = CsvSchema.builder()
    	        .addColumn("id")
    	        .addColumn("parentId")
    	        .addColumn("run")
    	        .addColumn("startProcessingTime")
    	        .addColumn("endProcessingTime")
    	        .addColumn("chargedCoins")
    	        .setUseHeader(true)
    	        .build();
		mapper = new CsvMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
	}
	
	@GetMapping("/jobs/{userId}/CSV")
	@PreAuthorize("hasAnyRole('ADMIN')")
    public void getJobReportForUser(@ModelAttribute("userId") User user , @RequestParam(name = "startDateTime", required = false) @DateTimeFormat(iso=ISO.DATE_TIME) OffsetDateTime  startDateTime, @RequestParam(name = "endDateTime", required = false) @DateTimeFormat(iso=ISO.DATE_TIME) OffsetDateTime endDateTime, HttpServletResponse response) throws IOException{
		try {
			Pair<LocalDateTime, LocalDateTime> startEndLocalDateTime = computeValidDateRange(startDateTime, endDateTime);
			LocalDateTime startLocalDateTime = startEndLocalDateTime.getLeft();
			LocalDateTime endLocalDateTime = startEndLocalDateTime.getRight();
			List<CsvJob> csvJobs = getUserJobs(user, startLocalDateTime, endLocalDateTime);
			response.addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + "job_report_" + DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(startLocalDateTime) + "_" +  DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(endLocalDateTime) + ".csv\"");
			response.setContentType("text/csv");
			mapper.writer(jobSchema).writeValuesAsArray(response.getOutputStream()).writeAll(csvJobs);
		}
    	catch (IllegalArgumentException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
		}
	}



	@GetMapping("/jobs/CSV")
	public void getJobReport(@RequestParam(name = "startDateTime", required = false) @DateTimeFormat(iso=ISO.DATE_TIME) OffsetDateTime  startDateTime, @RequestParam(name = "endDateTime", required = false) @DateTimeFormat(iso=ISO.DATE_TIME) OffsetDateTime endDateTime, HttpServletResponse response) throws IOException{
		User user = fstepSecurityService.getCurrentUser();
		try {
			Pair<LocalDateTime, LocalDateTime> startEndLocalDateTime = computeValidDateRange(startDateTime, endDateTime);
			LocalDateTime startLocalDateTime = startEndLocalDateTime.getLeft();
			LocalDateTime endLocalDateTime = startEndLocalDateTime.getRight();
			List<CsvJob> csvJobs = getUserJobs(user, startLocalDateTime, endLocalDateTime);
			response.addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + "job_report_" + DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(startLocalDateTime) + "_" +  DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(endLocalDateTime) + ".csv\"");
			response.setContentType("text/csv");
			mapper.writer(jobSchema).writeValuesAsArray(response.getOutputStream()).writeAll(csvJobs);
		}
		catch (IllegalArgumentException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
		}
	}
	
	
	//TODO this should go into a service class
	private List<CsvJob> getUserJobs(User user, LocalDateTime startLocalDateTime, LocalDateTime endLocalDateTime) {
		List<Job> jobs = jobDataService.findByOwnerAndParentFalseAndStartTimeBetween(user, startLocalDateTime, endLocalDateTime);
    	List<CsvJob> csvJobs = new ArrayList<>();
    	for(Job job: jobs) {
    		List<JobProcessing> jobProcessings = jobProcessingDataService.findByJobOrderBySequenceNumAsc(job);
    		for (JobProcessing jobProcessing: jobProcessings){
    			csvJobs.add(createCsvJob(job, jobProcessing));
    		}
    	}
    	return csvJobs;
	}

	
	private CsvJob createCsvJob(Job job, JobProcessing jobProcessing) {
		CsvJobBuilder csvJobBuilder = CsvJob.builder();
		csvJobBuilder.id(job.getId());
		if (job.getParentJob()!= null) {
			csvJobBuilder.parentId(job.getParentJob().getId());
		}
		csvJobBuilder.run(jobProcessing.getSequenceNum());
		csvJobBuilder.startProcessingTime(jobProcessing.getStartProcessingTime());
		csvJobBuilder.endProcessingTime(jobProcessing.getEndProcessingTime());
		csvJobBuilder.chargedCoins(getChargedCoinsForJobProcessing(jobProcessing));
		return csvJobBuilder.build();
		
	}
	
	private Pair<LocalDateTime, LocalDateTime> computeValidDateRange(OffsetDateTime startDateTime, OffsetDateTime endDateTime){
		LocalDateTime startLocalDateTime, endLocalDateTime;
		if (startDateTime != null && endDateTime == null) {
			startLocalDateTime = startDateTime.toLocalDateTime();
			endLocalDateTime = startLocalDateTime.plusMonths(1);
		}
		else if (startDateTime == null && endDateTime != null) {
			endLocalDateTime = endDateTime.toLocalDateTime();
			startLocalDateTime = endLocalDateTime.minusMonths(1);
		}
		else if (startDateTime == null && endDateTime ==null) {
			LocalDate now = LocalDate.now();
			startLocalDateTime = now.withDayOfMonth(1).atTime(LocalTime.MIN);
			endLocalDateTime = startLocalDateTime.plusMonths(1);
			
		}
		else {
			startLocalDateTime = startDateTime.toLocalDateTime();
			endLocalDateTime = endDateTime.toLocalDateTime();
			LocalDateTime maxEndLocalDateTime = startLocalDateTime.plusMonths(1);
			if (endLocalDateTime.isAfter(maxEndLocalDateTime)) {
				throw new IllegalArgumentException("Report interval must be less than 1 month");
			}
		}
		return ImmutablePair.of(startLocalDateTime, endLocalDateTime);
	}

	private int getChargedCoinsForJobProcessing(JobProcessing jobProcessing) {
		List<WalletTransaction> jobProcessingTransactions = walletTransactionDataService.findByTypeAndAssociatedId(WalletTransaction.Type.JOB_PROCESSING, jobProcessing.getId());
		//change sign of the balance change to get charged coins (a negative balance change is a positive coin charge)
		return jobProcessingTransactions.stream().mapToInt(t -> t.getBalanceChange() * -1).sum();
	}

}
