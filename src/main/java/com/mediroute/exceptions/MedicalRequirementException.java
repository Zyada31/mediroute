package com.mediroute.exceptions;


import com.mediroute.exceptions.BusinessException;
import com.mediroute.exceptions.ValidationException;
import java.util.Map;

public class MedicalRequirementException extends BusinessException {
    public MedicalRequirementException(String patientName, String requirement) {
        super("Patient " + patientName + " requires " + requirement + " but no compatible vehicle available",
                "MEDICAL_REQUIREMENT_NOT_MET");
    }
}

 class TimeWindowViolationException extends BusinessException {
    public TimeWindowViolationException(Long rideId, String timeWindow) {
        super("Ride " + rideId + " violates time window: " + timeWindow, "TIME_WINDOW_VIOLATION");
    }
}

 class AppointmentConflictException extends BusinessException {
    public AppointmentConflictException(Long patientId, String conflictDetails) {
        super("Appointment conflict for patient " + patientId + ": " + conflictDetails,
                "APPOINTMENT_CONFLICT");
    }
}

