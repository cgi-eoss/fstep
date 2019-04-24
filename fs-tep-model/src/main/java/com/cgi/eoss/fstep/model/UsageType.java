package com.cgi.eoss.fstep.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.fasterxml.jackson.annotation.JsonFormat;
@Getter
@AllArgsConstructor
@JsonFormat(shape=JsonFormat.Shape.OBJECT)
public enum UsageType {

	MAX_RUNNABLE_JOBS("MAX_RUNNABLE_JOBS", "Max Runnable Jobs", "jobs", 5L), 
	FILES_STORAGE_MB("FILES_STORAGE_MB", "Files Storage", "MB", 5000L),
	PERSISTENT_STORAGE_MB("PERSISTENT_STORAGE_MB", "Persistent folder storage", "MB", 0L);
	
	private final String name;
	private final String title;
	private final String unit;
	private final Long defaultValue;
	
}
