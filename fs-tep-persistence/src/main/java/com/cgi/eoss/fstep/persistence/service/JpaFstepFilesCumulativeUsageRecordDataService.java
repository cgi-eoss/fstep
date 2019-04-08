package com.cgi.eoss.fstep.persistence.service;

import static com.cgi.eoss.fstep.model.QFstepFilesCumulativeUsageRecord.fstepFilesCumulativeUsageRecord;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.FstepFilesCumulativeUsageRecord;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.persistence.dao.FstepEntityDao;
import com.cgi.eoss.fstep.persistence.dao.FstepFilesCumulativeUsageRecordDao;
import com.querydsl.core.types.Predicate;

@Service
@Transactional(readOnly = true)
public class JpaFstepFilesCumulativeUsageRecordDataService extends AbstractJpaDataService<FstepFilesCumulativeUsageRecord> implements FstepFilesCumulativeUsageRecordDataService{

	
	private FstepFilesCumulativeUsageRecordDao dao;

	@Autowired
	public JpaFstepFilesCumulativeUsageRecordDataService(FstepFilesCumulativeUsageRecordDao fstepFilesCumulativeUsageRecordDao) {
		this.dao = fstepFilesCumulativeUsageRecordDao;
	}
	
	@Override
	FstepEntityDao<FstepFilesCumulativeUsageRecord> getDao() {
		return dao;
	}

	@Override
	Predicate getUniquePredicate(FstepFilesCumulativeUsageRecord entity) {
		return  fstepFilesCumulativeUsageRecord.recordDate.eq(entity.getRecordDate())
				.and(fstepFilesCumulativeUsageRecord.owner.eq(entity.getOwner()))
				.and(getFiletypePredicate(entity));
	}
	
	private Predicate getFiletypePredicate(FstepFilesCumulativeUsageRecord entity) {
		return (entity.getFileType() == null)? fstepFilesCumulativeUsageRecord.fileType.isNull():fstepFilesCumulativeUsageRecord.fileType.eq(entity.getFileType());
	}

	@Override
	public FstepFilesCumulativeUsageRecord findTopByOwnerAndFileTypeIsNullAndRecordDateLessThanEqualOrderByRecordDateDesc(User owner, LocalDate date) {
		return dao.findTopByOwnerAndFileTypeIsNullAndRecordDateLessThanEqualOrderByRecordDateDesc(owner, date);
	}

	@Override
	public FstepFilesCumulativeUsageRecord findTopByOwnerAndFileTypeAndRecordDateLessThanEqualOrderByRecordDateDesc(User owner, FstepFile.Type fileType, LocalDate date) {
		return dao.findTopByOwnerAndFileTypeAndRecordDateLessThanEqualOrderByRecordDateDesc(owner, fileType, date);
	}
	@Override
	public List<FstepFilesCumulativeUsageRecord> findByOwnerAndFileTypeIsNullAndRecordDateBetween(User owner, LocalDate start, LocalDate end) {
		return dao.findByOwnerAndFileTypeIsNullAndRecordDateBetween(owner, start, end);
	}
	
	@Override
	public List<FstepFilesCumulativeUsageRecord> findByOwnerAndFileTypeAndRecordDateBetween(User owner, FstepFile.Type fileType, LocalDate start, LocalDate end) {
		return dao.findByOwnerAndFileTypeAndRecordDateBetween(owner, fileType, start, end);
	}

	@Override
	@Transactional
	public void updateUsageRecords(FstepFile fstepFile) {
		if (fstepFile.getFilesize() == null || fstepFile.getFilesize() == 0)
			return;
		LocalDate localDate = LocalDate.now();
		//Overall cumulative
		FstepFilesCumulativeUsageRecord latestOverallRecord = dao.findTopByOwnerAndFileTypeIsNullAndRecordDateLessThanEqualOrderByRecordDateDesc(fstepFile.getOwner(), localDate);
		long overallCumulativeSize = 0;
		if (latestOverallRecord != null) {
			overallCumulativeSize = latestOverallRecord.getCumulativeSize();
			
		}
		overallCumulativeSize+=fstepFile.getFilesize();
		FstepFilesCumulativeUsageRecord overallRecord = new FstepFilesCumulativeUsageRecord(fstepFile.getOwner(), localDate, null, overallCumulativeSize);
		save(overallRecord);
		
		//Per type cumulative
		if (fstepFile.getType() != null) {
			FstepFilesCumulativeUsageRecord latestPerTypeRecord = dao.findTopByOwnerAndFileTypeAndRecordDateLessThanEqualOrderByRecordDateDesc(fstepFile.getOwner(), fstepFile.getType(), localDate);
			long perTypeCumulativeSize = 0;
			if (latestPerTypeRecord != null) {
				perTypeCumulativeSize = latestPerTypeRecord.getCumulativeSize();
				
			}
			perTypeCumulativeSize+=fstepFile.getFilesize();
			FstepFilesCumulativeUsageRecord perTypeRecord = new FstepFilesCumulativeUsageRecord(fstepFile.getOwner(), localDate, fstepFile.getType(), perTypeCumulativeSize);
			save(perTypeRecord);
		}
		
	}

	
}
