package com.cgi.eoss.fstep.catalogue;

public class IngestionException extends CatalogueException {
    public IngestionException(String message) {
        super(message);
    }

    public IngestionException(Throwable cause) {
        super(cause);
    }

    public IngestionException(String message, Throwable cause) {
        super(message, cause);
    }
}
