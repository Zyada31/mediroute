package com.mediroute.exceptions;

public class PatientNotFoundException extends RuntimeException {

    public PatientNotFoundException(Long id) {
        super(String.valueOf(id));
    }

    public PatientNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}