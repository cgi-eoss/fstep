package com.cgi.eoss.fstep.worker;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.cgi.eoss.fstep.io.download.UnzipStrategy;

@Configuration
@ConfigurationProperties(prefix="unzipScheme")
public class UnzipBySchemeProperties {
	
	private final Map<String, UnzipStrategy> unzipByScheme = new HashMap<>();
	   
	public Map<String, UnzipStrategy> getUnzipByScheme() {
	        return unzipByScheme;
	}
}