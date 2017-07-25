package com.cgi.eoss.fstep.orchestrator.service;

/**
 * <p>Signals that an exception occurred in the execution of an FS-TEP Service.</p>
 */
public class ServiceExecutionException extends RuntimeException {

    /**
     * <p>Constructs a new service execution exception with the given detail message.</p>
     *
     * @param message
     */
    public ServiceExecutionException(String message) {
        super(message);
    }

}
