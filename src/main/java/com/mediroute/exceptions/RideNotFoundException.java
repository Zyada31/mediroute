// 3. Ride Exceptions
package com.mediroute.exceptions;

import com.mediroute.exceptions.ResourceNotFoundException;
import com.mediroute.exceptions.BusinessException;

public class RideNotFoundException extends ResourceNotFoundException {
    public RideNotFoundException(Long id) {
        super("Ride not found with ID: " + id);
    }
}

