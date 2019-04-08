package com.cgi.eoss.fstep.model.csv;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CsvStorageDailyUsage {

	private LocalDate date;
	
	private long usage;
}
