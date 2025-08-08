//package com.mediroute.service.ride;
//
//import com.mediroute.entity.Patient;
//import com.mediroute.entity.Ride;
//import org.springframework.stereotype.Component;
//
//@Component
//public class RideValidator {
//
//    public ValidationResult validate(Ride ride) {
//        ValidationResult result = new ValidationResult();
//
//        // Required fields
//        if (ride.getPatient() == null) {
//            result.addError("Patient is required");
//        }
//
//        if (ride.getPickupLocation() == null ||
//                !ride.getPickupLocation().isValid()) {
//            result.addError("Valid pickup location is required");
//        }
//
//        // Business rules
//        if (ride.getPickupTime() != null &&
//                ride.getPickupTime().isBefore(LocalDateTime.now())) {
//            result.addError("Pickup time cannot be in the past");
//        }
//
//        // Medical requirements
//        if (ride.getPatient() != null) {
//            validateMedicalRequirements(ride, result);
//        }
//
//        return result;
//    }
//
//    private void validateMedicalRequirements(Ride ride, ValidationResult result) {
//        Patient patient = ride.getPatient();
//
//        if (Boolean.TRUE.equals(patient.getRequiresWheelchair()) &&
//                !"wheelchair_van".equals(ride.getRequiredVehicleType())) {
//            result.addWarning("Patient requires wheelchair but vehicle type is " +
//                    ride.getRequiredVehicleType());
//        }
//
//        if (Boolean.TRUE.equals(patient.getRequiresOxygen()) &&
//                ride.getPickupDriver() != null &&
//                !Boolean.TRUE.equals(ride.getPickupDriver().getOxygenEquipped())) {
//            result.addError("Patient requires oxygen but assigned driver is not equipped");
//        }
//    }
//}
