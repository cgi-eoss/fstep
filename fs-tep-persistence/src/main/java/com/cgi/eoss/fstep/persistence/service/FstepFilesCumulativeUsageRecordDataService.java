package com.cgi.eoss.fstep.persistence.service;

import java.time.LocalDate;
import java.util.List;

import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.FstepFilesCumulativeUsageRecord;
import com.cgi.eoss.fstep.model.User;

public interface FstepFilesCumulativeUsageRecordDataService extends FstepEntityDataService<FstepFilesCumulativeUsageRecord> {
	
	public FstepFilesCumulativeUsageRecord findTopByOwnerAndFileTypeIsNullAndRecordDateLessThanEqualOrderByRecordDateDesc(User owner, LocalDate date);

	public FstepFilesCumulativeUsageRecord findTopByOwnerAndFileTypeAndRecordDateLessThanEqualOrderByRecordDateDesc(User owner, FstepFile.Type fileType, LocalDate date);
	
	public List<FstepFilesCumulativeUsageRecord> findByOwnerAndFileTypeIsNullAndRecordDateBetween(User owner, LocalDate start, LocalDate end);
	
	public List<FstepFilesCumulativeUsageRecord> findByOwnerAndFileTypeAndRecordDateBetween(User owner, FstepFile.Type fileType, LocalDate start, LocalDate end);

	public void updateUsageRecordsOnCreate(FstepFile fstepFile);
	
	public void updateUsageRecordsOnDelete(FstepFile fstepFile);
	
}
