package com.mediroute.exceptions;

/**
 * Exception thrown for business logic violations and rule enforcement
 */
public class BusinessException extends RuntimeException {

    private final String errorCode;
    private final Object[] parameters;

    public BusinessException(String message) {
        super(message);
        this.errorCode = null;
        this.parameters = null;
    }

    public BusinessException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.parameters = null;
    }

    public BusinessException(String message, String errorCode, Object... parameters) {
        super(message);
        this.errorCode = errorCode;
        this.parameters = parameters;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
        this.parameters = null;
    }

    public BusinessException(String message, Throwable cause, String errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
        this.parameters = null;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Object[] getParameters() {
        return parameters;
    }
}
