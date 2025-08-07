package com.mediroute.exceptions;

import java.util.Map;

/**
 * Exception for custom validation errors beyond standard Bean Validation
 */
public class ValidationException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public ValidationException(String message) {
        super(message);
        this.fieldErrors = null;
    }

    public ValidationException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors;
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
        this.fieldErrors = null;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}