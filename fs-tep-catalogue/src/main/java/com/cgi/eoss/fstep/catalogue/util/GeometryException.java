package com.cgi.eoss.fstep.catalogue.util;

/**
 */
public class GeometryException extends RuntimeException {
    public GeometryException(Exception e) {
        super(e);
    }

	public GeometryException(String message) {
		super(message);
	}
	
	public GeometryException(String message, Throwable t) {
		super(message, t);
	}
	
}
