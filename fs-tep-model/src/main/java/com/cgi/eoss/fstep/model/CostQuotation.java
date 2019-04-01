package com.cgi.eoss.fstep.model;

import lombok.Data;

@Data
public class CostQuotation {

	
	public enum Recurrence {
		ONE_OFF, HOURLY, DAILY, MONTHLY;
	}

	private final Integer cost;
	
	private final Recurrence recurrence;
}

