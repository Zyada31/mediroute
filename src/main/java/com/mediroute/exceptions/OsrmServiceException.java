package com.mediroute.exceptions;

public class OsrmServiceException extends RuntimeException {
    public OsrmServiceException(String message) {
        super("OSRM service error: " + message);
    }

    public OsrmServiceException(String message, Throwable cause) {
        super("OSRM service error: " + message, cause);
    }
}
