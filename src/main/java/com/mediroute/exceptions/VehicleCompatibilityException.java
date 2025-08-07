package com.mediroute.exceptions;

public class VehicleCompatibilityException extends BusinessException {
    public VehicleCompatibilityException(String patientRequirement, String driverCapability) {
        super("Vehicle incompatible: requires " + patientRequirement + " but driver has " + driverCapability,
                "VEHICLE_INCOMPATIBLE");
    }
}
