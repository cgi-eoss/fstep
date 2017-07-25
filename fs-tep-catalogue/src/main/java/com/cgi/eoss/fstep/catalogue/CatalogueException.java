package com.cgi.eoss.fstep.catalogue;

public class CatalogueException extends RuntimeException {
    public CatalogueException(String message) {
        super(message);
    }

    public CatalogueException(Throwable cause) {
        super(cause);
    }

    public CatalogueException(String message, Throwable cause) {
        super(message, cause);
    }
}
