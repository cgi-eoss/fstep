package com.cgi.eoss.fstep.orchestrator.service;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

@Data
class TraefikProxyConfig {
	private Map<String, Object> frontends = new HashMap<>();
	private Map<String, Object> backends = new HashMap<>();
}
