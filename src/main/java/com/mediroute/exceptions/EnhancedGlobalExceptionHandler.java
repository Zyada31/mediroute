package com.mediroute.exceptions;

import com.mediroute.exceptions.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class EnhancedGlobalExceptionHandler extends GlobalExceptionHandler {

    private static final String PROBLEM_BASE_URL = "https://api.mediroute.com/problems/";

    // Medical Transport Specific Exception Handlers

    @ExceptionHandler(PatientNotFoundException.class)
    public ProblemDetail handlePatientNotFoundException(PatientNotFoundException ex) {
        log.warn("Patient not found: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Patient Not Found");
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "patient-not-found"));
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        return problemDetail;
    }

    @ExceptionHandler(com.mediroute.exception.DriverNotFoundException.class)
    public ProblemDetail handleDriverNotFoundException(com.mediroute.exception.DriverNotFoundException ex) {
        log.warn("Driver not found: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Driver Not Found");
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "driver-not-found"));
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        return problemDetail;
    }

    @ExceptionHandler(RideNotFoundException.class)
    public ProblemDetail handleRideNotFoundException(RideNotFoundException ex) {
        log.warn("Ride not found: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Ride Not Found");
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "ride-not-found"));
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        return problemDetail;
    }

    @ExceptionHandler(OptimizationException.class)
    public ProblemDetail handleOptimizationException(OptimizationException ex) {
        log.error("Optimization error: {}", ex.getMessage(), ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        problemDetail.setTitle("Optimization Error");
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "optimization-error"));
        problemDetail.setProperty("timestamp", LocalDateTime.now());

        if (ex.getErrorCode() != null) {
            problemDetail.setProperty("errorCode", ex.getErrorCode());
        }

        return problemDetail;
    }

    @ExceptionHandler(NoDriversAvailableException.class)
    public ProblemDetail handleNoDriversAvailableException(NoDriversAvailableException ex) {
        log.warn("No drivers available: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("No Drivers Available");
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "no-drivers-available"));
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        problemDetail.setProperty("errorCode", ex.getErrorCode());
        return problemDetail;
    }

    @ExceptionHandler(RideAssignmentException.class)
    public ProblemDetail handleRideAssignmentException(RideAssignmentException ex) {
        log.warn("Ride assignment failed: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("Ride Assignment Failed");
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "ride-assignment-failed"));
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        problemDetail.setProperty("errorCode", ex.getErrorCode());
        return problemDetail;
    }

    @ExceptionHandler(MedicalRequirementException.class)
    public ProblemDetail handleMedicalRequirementException(MedicalRequirementException ex) {
        log.warn("Medical requirement not met: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("Medical Requirement Not Met");
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "medical-requirement-not-met"));
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        problemDetail.setProperty("errorCode", ex.getErrorCode());
        return problemDetail;
    }

    @ExceptionHandler(VehicleCompatibilityException.class)
    public ProblemDetail handleVehicleCompatibilityException(VehicleCompatibilityException ex) {
        log.warn("Vehicle compatibility issue: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("Vehicle Incompatible");
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "vehicle-incompatible"));
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        problemDetail.setProperty("errorCode", ex.getErrorCode());
        return problemDetail;
    }

    @ExceptionHandler(TimeWindowViolationException.class)
    public ProblemDetail handleTimeWindowViolationException(TimeWindowViolationException ex) {
        log.warn("Time window violation: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("Time Window Violation");
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "time-window-violation"));
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        problemDetail.setProperty("errorCode", ex.getErrorCode());
        return problemDetail;
    }

    @ExceptionHandler(ExcelParsingException.class)
    public ProblemDetail handleExcelParsingException(ExcelParsingException ex) {
        log.error("Excel parsing error: {}", ex.getMessage(), ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Excel Parsing Error");
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "excel-parsing-error"));
        problemDetail.setProperty("timestamp", LocalDateTime.now());

        if (ex.getFileName() != null) {
            problemDetail.setProperty("fileName", ex.getFileName());
        }
        if (ex.getLineNumber() > 0) {
            problemDetail.setProperty("lineNumber", ex.getLineNumber());
        }
        if (ex.getFieldName() != null) {
            problemDetail.setProperty("fieldName", ex.getFieldName());
        }

        return problemDetail;
    }

    @ExceptionHandler(CsvParsingException.class)
    public ProblemDetail handleCsvParsingException(CsvParsingException ex) {
        log.error("CSV parsing error: {}", ex.getMessage(), ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("CSV Parsing Error");
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "csv-parsing-error"));
        problemDetail.setProperty("timestamp", LocalDateTime.now());

        if (ex.getFileName() != null) {
            problemDetail.setProperty("fileName", ex.getFileName());
        }
        if (ex.getLineNumber() > 0) {
            problemDetail.setProperty("lineNumber", ex.getLineNumber());
        }
        if (ex.getFieldName() != null) {
            problemDetail.setProperty("fieldName", ex.getFieldName());
        }

        return problemDetail;
    }

    @ExceptionHandler(GeocodingException.class)
    public ProblemDetail handleGeocodingException(GeocodingException ex) {
        log.error("Geocoding error: {}", ex.getMessage(), ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Geocoding Error");
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "geocoding-error"));
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        problemDetail.setProperty("address", ex.getAddress());
        return problemDetail;
    }

    @ExceptionHandler(DistanceCalculationException.class)
    public ProblemDetail handleDistanceCalculationException(DistanceCalculationException ex) {
        log.error("Distance calculation error: {}", ex.getMessage(), ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Distance Calculation Error");
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "distance-calculation-error"));
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        problemDetail.setProperty("origin", ex.getOrigin());
        problemDetail.setProperty("destination", ex.getDestination());
        problemDetail.setProperty("errorCode", ex.getErrorCode());
        return problemDetail;
    }

    @ExceptionHandler(OsrmServiceException.class)
    public ProblemDetail handleOsrmServiceException(OsrmServiceException ex) {
        log.error("OSRM service error: {}", ex.getMessage(), ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_GATEWAY, ex.getMessage());
        problemDetail.setTitle("External Service Error");
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "osrm-service-error"));
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        return problemDetail;
    }

    @ExceptionHandler(DriverNotQualifiedException.class)
    public ProblemDetail handleDriverNotQualifiedException(DriverNotQualifiedException ex) {
        log.warn("Driver not qualified: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("Driver Not Qualified");
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "driver-not-qualified"));
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        problemDetail.setProperty("errorCode", ex.getErrorCode());
        return problemDetail;
    }

    @ExceptionHandler(RideSchedulingException.class)
    public ProblemDetail handleRideSchedulingException(RideSchedulingException ex) {
        log.warn("Ride scheduling error: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("Ride Scheduling Error");
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "ride-scheduling-error"));
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        problemDetail.setProperty("errorCode", ex.getErrorCode());
        return problemDetail;
    }
}

