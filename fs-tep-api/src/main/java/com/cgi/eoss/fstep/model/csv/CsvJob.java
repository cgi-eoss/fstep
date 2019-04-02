package com.cgi.eoss.fstep.model.csv;

import java.time.OffsetDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CsvJob {

	private long id;
	
	private Long parentId;
	
	private long run;
	
	private OffsetDateTime startProcessingTime;
	
	private OffsetDateTime endProcessingTime;
	
	private int chargedCoins;
	
}
