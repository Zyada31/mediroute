package com.mediroute.exceptions;

public class RideAssignmentException extends BusinessException {
    public RideAssignmentException(Long rideId, String reason) {
        super("Cannot assign ride " + rideId + ": " + reason, "RIDE_ASSIGNMENT_FAILED");
    }
}
