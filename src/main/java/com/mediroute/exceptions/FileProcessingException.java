package com.mediroute.exceptions;

/**
 * Exception thrown during file processing operations (CSV, Excel imports)
 */
public class FileProcessingException extends RuntimeException {

    private final String fileName;
    private final int lineNumber;
    private final String fieldName;

    public FileProcessingException(String message) {
        super(message);
        this.fileName = null;
        this.lineNumber = -1;
        this.fieldName = null;
    }

    public FileProcessingException(String message, String fileName) {
        super(message);
        this.fileName = fileName;
        this.lineNumber = -1;
        this.fieldName = null;
    }

    public FileProcessingException(String message, String fileName, int lineNumber) {
        super(message);
        this.fileName = fileName;
        this.lineNumber = lineNumber;
        this.fieldName = null;
    }

    public FileProcessingException(String message, String fileName, int lineNumber, String fieldName) {
        super(message);
        this.fileName = fileName;
        this.lineNumber = lineNumber;
        this.fieldName = fieldName;
    }

    public FileProcessingException(String message, Throwable cause) {
        super(message, cause);
        this.fileName = null;
        this.lineNumber = -1;
        this.fieldName = null;
    }

    public String getFileName() {
        return fileName;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getFieldName() {
        return fieldName;
    }
}