package com.mediroute.exceptions;

public final class ErrorCodes {

    // Patient related errors
    public static final String PATIENT_NOT_FOUND = "PATIENT_NOT_FOUND";
    public static final String PATIENT_DUPLICATE = "PATIENT_DUPLICATE";
    public static final String PATIENT_INACTIVE = "PATIENT_INACTIVE";

    // Driver related errors
    public static final String DRIVER_NOT_FOUND = "DRIVER_NOT_FOUND";
    public static final String DRIVER_NOT_QUALIFIED = "DRIVER_NOT_QUALIFIED";
    public static final String DRIVER_UNAVAILABLE = "DRIVER_UNAVAILABLE";
    public static final String VEHICLE_MISMATCH = "VEHICLE_MISMATCH";

    // Ride related errors
    public static final String RIDE_NOT_FOUND = "RIDE_NOT_FOUND";
    public static final String RIDE_ASSIGNMENT_FAILED = "RIDE_ASSIGNMENT_FAILED";
    public static final String TIME_CONFLICT = "TIME_CONFLICT";
    public static final String TIME_WINDOW_VIOLATION = "TIME_WINDOW_VIOLATION";

    // Optimization errors
    public static final String OPTIMIZATION_FAILED = "OPTIMIZATION_FAILED";
    public static final String NO_DRIVERS_AVAILABLE = "NO_DRIVERS_AVAILABLE";
    public static final String CONSTRAINT_VIOLATION = "CONSTRAINT_VIOLATION";
    public static final String BATCH_OPTIMIZATION_FAILED = "BATCH_OPTIMIZATION_FAILED";

    // Medical transport errors
    public static final String MEDICAL_REQUIREMENT_NOT_MET = "MEDICAL_REQUIREMENT_NOT_MET";
    public static final String VEHICLE_INCOMPATIBLE = "VEHICLE_INCOMPATIBLE";
    public static final String APPOINTMENT_CONFLICT = "APPOINTMENT_CONFLICT";

    // File processing errors
    public static final String EXCEL_PARSING_ERROR = "EXCEL_PARSING_ERROR";
    public static final String CSV_PARSING_ERROR = "CSV_PARSING_ERROR";
    public static final String GEOCODING_ERROR = "GEOCODING_ERROR";
    public static final String DISTANCE_CALCULATION_FAILED = "DISTANCE_CALCULATION_FAILED";

    // External service errors
    public static final String OSRM_SERVICE_ERROR = "OSRM_SERVICE_ERROR";
    public static final String GOOGLE_API_ERROR = "GOOGLE_API_ERROR";

    private ErrorCodes() {
        // Utility class
    }
}
