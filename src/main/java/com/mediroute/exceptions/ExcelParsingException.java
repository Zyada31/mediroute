// ============================================================================
// ENHANCED EXCEPTION SYSTEM - Medical Transport Specific
// ============================================================================

// 1. Enhanced Patient Exceptions
package com.mediroute.exceptions;


import com.mediroute.exceptions.FileProcessingException;

public class ExcelParsingException extends FileProcessingException {
    public ExcelParsingException(String message, String fileName, int lineNumber) {
        super(message, fileName, lineNumber);
    }

    public ExcelParsingException(String message, String fileName, int lineNumber, String fieldName) {
        super(message, fileName, lineNumber, fieldName);
    }

    public ExcelParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}


