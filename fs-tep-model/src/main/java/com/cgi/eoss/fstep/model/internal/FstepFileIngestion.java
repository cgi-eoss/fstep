package com.cgi.eoss.fstep.model.internal;

import com.cgi.eoss.fstep.model.FstepFile;

import lombok.Data;

@Data
public class FstepFileIngestion {
	
	private final String statusMessage;
	
	private final FstepFile fstepFile;

}
