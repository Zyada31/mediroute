package com.mediroute.exceptions;

public class ConstraintViolationException extends OptimizationException {
    public ConstraintViolationException(String constraint, String details) {
        super("Constraint violation: " + constraint + " - " + details, "CONSTRAINT_VIOLATION");
    }
}
