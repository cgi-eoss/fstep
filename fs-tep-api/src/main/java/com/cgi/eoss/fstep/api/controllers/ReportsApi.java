package com.cgi.eoss.fstep.api.controllers;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

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

import com.cgi.eoss.fstep.model.FstepFilesCumulativeUsageRecord;
import com.cgi.eoss.fstep.model.Job;
import com.cgi.eoss.fstep.model.JobProcessing;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.model.WalletTransaction;
import com.cgi.eoss.fstep.model.csv.CsvJob;
import com.cgi.eoss.fstep.model.csv.CsvJob.CsvJobBuilder;
import com.cgi.eoss.fstep.model.csv.CsvStorageDailyUsage;
import com.cgi.eoss.fstep.persistence.service.FstepFilesCumulativeUsageRecordDataService;
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
	private final FstepFilesCumulativeUsageRecordDataService fstepFilesCumulativeUsageRecordDataService;
	private CsvSchema jobSchema;
	private CsvSchema storageSchema;
	private CsvMapper mapper;
	@Autowired
	public ReportsApi(FstepSecurityService fstepSecurityService, JobDataService jobDataService, JobProcessingDataService jobProcessingDataService, WalletTransactionDataService walletTransactionDataService, FstepFilesCumulativeUsageRecordDataService fstepFilesCumulativeUsageRecordDataService) {
		this.fstepSecurityService = fstepSecurityService;
		this.jobDataService = jobDataService;
		this.jobProcessingDataService = jobProcessingDataService;
		this.walletTransactionDataService = walletTransactionDataService;
		this.fstepFilesCumulativeUsageRecordDataService = fstepFilesCumulativeUsageRecordDataService;
		jobSchema = CsvSchema.builder()
    	        .addColumn("id")
    	        .addColumn("parentId")
    	        .addColumn("run")
    	        .addColumn("startProcessingTime")
    	        .addColumn("endProcessingTime")
    	        .addColumn("chargedCoins")
    	        .setUseHeader(true)
    	        .build();
		storageSchema = CsvSchema.builder()
    	        .addColumn("date")
    	        .addColumn("usage")
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
			Pair<OffsetDateTime, OffsetDateTime> startEndLocalDateTime = computeValidDateRange(startDateTime, endDateTime);
			LocalDateTime startLocalDateTime = startEndLocalDateTime.getLeft().toLocalDateTime();
			LocalDateTime endLocalDateTime = startEndLocalDateTime.getRight().toLocalDateTime();
			List<CsvJob> csvJobs = getUserJobs(user, startLocalDateTime, endLocalDateTime);
			response.addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + "fstep_job_report_user_" + user.getId() + "_" + DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(startLocalDateTime) + "_" +  DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(endLocalDateTime) + ".csv\"");
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
			Pair<OffsetDateTime, OffsetDateTime> startEndLocalDateTime = computeValidDateRange(startDateTime, endDateTime);
			LocalDateTime startLocalDateTime = startEndLocalDateTime.getLeft().toLocalDateTime();
			LocalDateTime endLocalDateTime = startEndLocalDateTime.getRight().toLocalDateTime();
			List<CsvJob> csvJobs = getUserJobs(user, startLocalDateTime, endLocalDateTime);
			response.addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + "fstep_job_report_" + DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(startLocalDateTime) + "_" +  DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(endLocalDateTime) + ".csv\"");
			response.setContentType("text/csv");
			mapper.writer(jobSchema).writeValuesAsArray(response.getOutputStream()).writeAll(csvJobs);
		}
		catch (IllegalArgumentException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
		}
	}
	
	@GetMapping("/storage/{userId}/CSV")
	@PreAuthorize("hasAnyRole('ADMIN')")
    public void getStorageReportForUser(@ModelAttribute("userId") User user , @RequestParam(name = "startDateTime", required = false) @DateTimeFormat(iso=ISO.DATE_TIME) OffsetDateTime  startDateTime, @RequestParam(name = "endDateTime", required = false) @DateTimeFormat(iso=ISO.DATE_TIME) OffsetDateTime endDateTime, HttpServletResponse response) throws IOException{
		try {
			Pair<OffsetDateTime, OffsetDateTime> startEndDateTime = computeValidDateRange(startDateTime, endDateTime);
			List<CsvStorageDailyUsage> csvStorageDailyUsage = getUserStorageDailyUsage(user, startEndDateTime.getLeft(), startEndDateTime.getRight());
			response.addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + "fstep_storage_report_user_" + user.getId() + "_" + DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(startEndDateTime.getLeft().toLocalDateTime()) + "_" +  DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(startEndDateTime.getRight().toLocalDateTime()) + ".csv\"");
			response.setContentType("text/csv");
			mapper.writer(storageSchema).writeValuesAsArray(response.getOutputStream()).writeAll(csvStorageDailyUsage);
		}
    	catch (IllegalArgumentException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
		}
	}

	@GetMapping("/storage/CSV")
	public void getStorageReport(@RequestParam(name = "startDateTime", required = false) @DateTimeFormat(iso=ISO.DATE_TIME) OffsetDateTime  startDateTime, @RequestParam(name = "endDateTime", required = false) @DateTimeFormat(iso=ISO.DATE_TIME) OffsetDateTime endDateTime, HttpServletResponse response) throws IOException{
		User user = fstepSecurityService.getCurrentUser();
		try {
			Pair<OffsetDateTime, OffsetDateTime> startEndDateTime = computeValidDateRange(startDateTime, endDateTime);
			List<CsvStorageDailyUsage> csvStorageDailyUsage = getUserStorageDailyUsage(user, startEndDateTime.getLeft(), startEndDateTime.getRight());
			response.addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + "fstep_storage_report_" + DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(startEndDateTime.getLeft().toLocalDateTime()) + "_" +  DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(startEndDateTime.getRight().toLocalDateTime()) + ".csv\"");
			response.setContentType("text/csv");
			mapper.writer(storageSchema).writeValuesAsArray(response.getOutputStream()).writeAll(csvStorageDailyUsage);
		}
		catch (IllegalArgumentException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
		}
	}

	//TODO this should go into a service class
	private List<CsvStorageDailyUsage> getUserStorageDailyUsage(User user, OffsetDateTime startDateTime,
			OffsetDateTime endDateTime) {
		TreeMap<LocalDate, Long> orderedUsage = new TreeMap<LocalDate, Long>();
		FstepFilesCumulativeUsageRecord usageRecord = fstepFilesCumulativeUsageRecordDataService.findTopByOwnerAndFileTypeIsNullAndRecordDateLessThanEqualOrderByRecordDateDesc(user, startDateTime.toLocalDate());
        if (usageRecord == null) {
        	orderedUsage.put(startDateTime.minusDays(1).toLocalDate(), 0L);
        }
		List<FstepFilesCumulativeUsageRecord> records = fstepFilesCumulativeUsageRecordDataService.findByOwnerAndFileTypeIsNullAndRecordDateBetween(user, startDateTime.toLocalDate(), endDateTime.toLocalDate());
		records.stream().forEach(r -> orderedUsage.put(r.getRecordDate(), r.getCumulativeSize()));
		LocalDate next = startDateTime.toLocalDate();
		List<CsvStorageDailyUsage> storageDailyUsage = new ArrayList<>();
		while (next.isAfter(endDateTime.toLocalDate()) == false) {
			storageDailyUsage.add(new CsvStorageDailyUsage(next, orderedUsage.floorEntry(next).getValue()));
			next = next.plusDays(1);
		}
		return storageDailyUsage;
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
	
	private Pair<OffsetDateTime, OffsetDateTime> computeValidDateRange(OffsetDateTime startDateTime, OffsetDateTime endDateTime){
		OffsetDateTime validStartDateTime, validEndDateTime;
		if (startDateTime != null && endDateTime == null) {
			validStartDateTime = startDateTime;
			validEndDateTime = validStartDateTime.plusMonths(1);
		}
		else if (startDateTime == null && endDateTime != null) {
			validEndDateTime = endDateTime;
			validStartDateTime = validEndDateTime.minusMonths(1);
		}
		else if (startDateTime == null && endDateTime ==null) {
			LocalDate now = LocalDate.now();
			validStartDateTime = now.withDayOfMonth(1).atTime(LocalTime.MIN).atOffset(ZoneOffset.UTC);
			validEndDateTime = validStartDateTime.plusMonths(1);
			
		}
		else {
			validStartDateTime = startDateTime;
			validEndDateTime = endDateTime;
			OffsetDateTime maxEndDateTime = validStartDateTime.plusMonths(1);
			if (validEndDateTime.isAfter(maxEndDateTime)) {
				throw new IllegalArgumentException("Report interval must be less than 1 month");
			}
		}
		return ImmutablePair.of(validStartDateTime, validEndDateTime);
	}
	
	private int getChargedCoinsForJobProcessing(JobProcessing jobProcessing) {
		List<WalletTransaction> jobProcessingTransactions = walletTransactionDataService.findByTypeAndAssociatedId(WalletTransaction.Type.JOB_PROCESSING, jobProcessing.getId());
		//change sign of the balance change to get charged coins (a negative balance change is a positive coin charge)
		return jobProcessingTransactions.stream().mapToInt(t -> t.getBalanceChange() * -1).sum();
	}

}
