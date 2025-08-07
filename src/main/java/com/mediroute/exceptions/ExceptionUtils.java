
// 9. Exception Utility Class
package com.mediroute.exceptions;

import com.mediroute.entity.Driver;
import com.mediroute.entity.Patient;
import com.mediroute.entity.Ride;
import com.mediroute.exceptions.*;

public class ExceptionUtils {

    public static void validatePatientExists(Patient patient, Long patientId) {
        if (patient == null) {
            throw new PatientNotFoundException(patientId);
        }
    }

    public static void validateDriverExists(Driver driver, Long driverId) {
        if (driver == null) {
            throw new com.mediroute.exception.DriverNotFoundException(driverId);
        }
    }

    public static void validateRideExists(Ride ride, Long rideId) {
        if (ride == null) {
            throw new RideNotFoundException(rideId);
        }
    }

    public static void validateDriverCapability(Driver driver, Patient patient) {
        if (patient.getRequiresWheelchair() && !driver.getWheelchairAccessible()) {
            throw new VehicleCompatibilityException("wheelchair accessibility", "standard vehicle");
        }

        if (patient.getRequiresStretcher() && !driver.getStretcherCapable()) {
            throw new VehicleCompatibilityException("stretcher capability", "non-stretcher vehicle");
        }

        if (patient.getRequiresOxygen() && !driver.getOxygenEquipped()) {
            throw new VehicleCompatibilityException("oxygen equipment", "non-oxygen equipped vehicle");
        }
    }

    public static void validateDriverAvailability(Driver driver) {
        if (!driver.getActive()) {
            throw new DriverUnavailableException(driver.getId(), "driver is inactive");
        }

        if (!driver.getIsTrainingComplete()) {
            throw new DriverNotQualifiedException(driver.getId(), "training not complete");
        }
    }

    public static void validateTimeWindow(Ride ride) {
        if (ride.getPickupWindowStart() != null && ride.getPickupWindowEnd() != null) {
            if (ride.getPickupTime().isBefore(ride.getPickupWindowStart()) ||
                    ride.getPickupTime().isAfter(ride.getPickupWindowEnd())) {
                throw new TimeWindowViolationException(ride.getId(),
                        "pickup time " + ride.getPickupTime() + " outside window " +
                                ride.getPickupWindowStart() + " - " + ride.getPickupWindowEnd());
            }
        }
    }
}