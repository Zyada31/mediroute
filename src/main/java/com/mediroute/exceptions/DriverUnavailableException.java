package com.mediroute.exceptions;

public class DriverUnavailableException extends BusinessException {
    public DriverUnavailableException(Long driverId, String reason) {
        super("Driver " + driverId + " is unavailable: " + reason, "DRIVER_UNAVAILABLE");
    }
}
