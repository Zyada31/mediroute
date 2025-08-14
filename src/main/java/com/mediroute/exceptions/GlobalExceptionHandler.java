package com.mediroute.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String PROBLEM_BASE_URL = "https://api.medicalrides.com/problems/";

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Resource Not Found");
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "resource-not-found"));
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        return problemDetail;
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail handleDuplicateResourceException(DuplicateResourceException ex) {
        log.warn("Duplicate resource: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("Resource Already Exists");
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "duplicate-resource"));
        problemDetail.setProperty("timestamp", LocalDateTime.now());

        if (ex.getField() != null) {
            problemDetail.setProperty("field", ex.getField());
        }
        if (ex.getValue() != null) {
            problemDetail.setProperty("value", ex.getValue());
        }

        return problemDetail;
    }

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusinessException(BusinessException ex) {
        log.warn("Business rule violation: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Business Rule Violation");
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "business-rule"));
        problemDetail.setProperty("timestamp", LocalDateTime.now());

        if (ex.getErrorCode() != null) {
            problemDetail.setProperty("errorCode", ex.getErrorCode());
        }
        if (ex.getParameters() != null) {
            problemDetail.setProperty("parameters", ex.getParameters());
        }

        return problemDetail;
    }

    @ExceptionHandler(ValidationException.class)
    public ProblemDetail handleValidationException(ValidationException ex) {
        log.warn("Custom validation error: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Validation Error");
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "validation"));
        problemDetail.setProperty("timestamp", LocalDateTime.now());

        if (ex.getFieldErrors() != null) {
            problemDetail.setProperty("fieldErrors", ex.getFieldErrors());
        }

        return problemDetail;
    }

    @ExceptionHandler(FileProcessingException.class)
    public ProblemDetail handleFileProcessingException(FileProcessingException ex) {
        log.error("File processing error: {}", ex.getMessage(), ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("File Processing Error");
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "file-processing"));
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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        log.warn("Bean validation error: {}", ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Validation failed");
        problemDetail.setTitle("Validation Error");
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "validation"));
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        problemDetail.setProperty("validationErrors", errors);
        return problemDetail;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        log.warn("File size exceeded: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "File size exceeds maximum allowed size");
        problemDetail.setTitle("File Size Exceeded");
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "file-size-exceeded"));
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        problemDetail.setProperty("maxSize", ex.getMaxUploadSize());
        return problemDetail;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Invalid argument: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Invalid Argument");
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "invalid-argument"));
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        return problemDetail;
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalStateException(IllegalStateException ex) {
        log.warn("Invalid state: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("Invalid State");
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "invalid-state"));
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "internal-error"));
        problemDetail.setProperty("timestamp", LocalDateTime.now());

        // Only include exception details in development
        if (isDevelopmentMode()) {
            problemDetail.setProperty("exceptionType", ex.getClass().getSimpleName());
            problemDetail.setProperty("exceptionMessage", ex.getMessage());
        }

        return problemDetail;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied");
        pd.setTitle("Forbidden");
        pd.setType(URI.create(PROBLEM_BASE_URL + "forbidden"));
        pd.setProperty("timestamp", LocalDateTime.now());
        return pd;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatusException(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, ex.getReason() != null ? ex.getReason() : ex.getMessage());
        pd.setTitle(status.is4xxClientError() ? "Request Error" : "Server Error");
        pd.setType(URI.create(PROBLEM_BASE_URL + "http-" + status.value()));
        pd.setProperty("timestamp", LocalDateTime.now());
        return pd;
    }

    /**
     * Check if application is running in development mode
     * You can customize this based on your environment detection logic
     */
    private boolean isDevelopmentMode() {
        String profile = System.getProperty("spring.profiles.active", "");
        return profile.contains("dev") || profile.contains("local");
    }
}