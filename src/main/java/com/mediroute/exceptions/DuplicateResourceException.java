package com.mediroute.exceptions;

/**
 * Exception thrown when attempting to create a resource that already exists
 * or violates unique constraints
 */
public class DuplicateResourceException extends RuntimeException {

    private final String field;
    private final String value;

    public DuplicateResourceException(String message) {
        super(message);
        this.field = null;
        this.value = null;
    }

    public DuplicateResourceException(String message, String field, String value) {
        super(message);
        this.field = field;
        this.value = value;
    }

    public DuplicateResourceException(String message, Throwable cause) {
        super(message, cause);
        this.field = null;
        this.value = null;
    }

    public String getField() {
        return field;
    }

    public String getValue() {
        return value;
    }
}