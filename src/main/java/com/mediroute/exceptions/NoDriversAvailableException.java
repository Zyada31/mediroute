package com.mediroute.exceptions;

public class NoDriversAvailableException extends OptimizationException {
    public NoDriversAvailableException() {
        super("No qualified drivers available for optimization", "NO_DRIVERS_AVAILABLE");
    }

    public NoDriversAvailableException(String vehicleType) {
        super("No drivers available with " + vehicleType + " capability", "NO_COMPATIBLE_DRIVERS");
    }
}
