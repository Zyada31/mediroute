package com.mediroute.exceptions;

public class DistanceCalculationException extends BusinessException {
    private final String origin;
    private final String destination;

    public DistanceCalculationException(String origin, String destination, String message) {
        super("Failed to calculate distance from '" + origin + "' to '" + destination + "': " + message,
                "DISTANCE_CALCULATION_FAILED");
        this.origin = origin;
        this.destination = destination;
    }

    public DistanceCalculationException(String origin, String destination, Throwable cause) {
        super("Failed to calculate distance from '" + origin + "' to '" + destination + "'",
                cause, "DISTANCE_SERVICE_ERROR");
        this.origin = origin;
        this.destination = destination;
    }

    public String getOrigin() {
        return origin;
    }

    public String getDestination() {
        return destination;
    }
}
