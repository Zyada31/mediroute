package com.mediroute.exception;

import com.mediroute.exceptions.ResourceNotFoundException;
import com.mediroute.exceptions.BusinessException;

public class DriverNotFoundException extends ResourceNotFoundException {
    public DriverNotFoundException(Long id) {
        super("Driver not found with ID: " + id);
    }
}

