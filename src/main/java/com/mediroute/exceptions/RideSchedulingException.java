package com.mediroute.exceptions;

public class RideSchedulingException extends BusinessException {
    public RideSchedulingException(String message) {
        super(message, "RIDE_SCHEDULING_ERROR");
    }

    public RideSchedulingException(Long rideId, String timeConflict) {
        super("Ride " + rideId + " has scheduling conflict: " + timeConflict, "TIME_CONFLICT");
    }
}
