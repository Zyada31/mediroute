package com.mediroute.exceptions;

public class DriverNotQualifiedException extends BusinessException {
    public DriverNotQualifiedException(Long driverId, String reason) {
        super("Driver " + driverId + " is not qualified: " + reason, "DRIVER_NOT_QUALIFIED");
    }

    public DriverNotQualifiedException(String driverName, String vehicleRequirement) {
        super("Driver " + driverName + " cannot handle " + vehicleRequirement + " requirements",
                "VEHICLE_MISMATCH");
    }
}
