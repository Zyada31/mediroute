// 4. Optimization Exceptions
package com.mediroute.exceptions;

import com.mediroute.exceptions.BusinessException;

public class OptimizationException extends BusinessException {
    public OptimizationException(String message) {
        super(message, "OPTIMIZATION_FAILED");
    }

    public OptimizationException(String message, Throwable cause) {
        super(message, cause, "OPTIMIZATION_ERROR");
    }

    public OptimizationException(String batchId, String reason) {
        super("Optimization failed for batch " + batchId + ": " + reason, "BATCH_OPTIMIZATION_FAILED");
    }
}

