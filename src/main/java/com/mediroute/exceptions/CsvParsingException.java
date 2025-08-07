package com.mediroute.exceptions;

public class CsvParsingException extends FileProcessingException {
    public CsvParsingException(String message, String fileName, int lineNumber) {
        super(message, fileName, lineNumber);
    }

    public CsvParsingException(String message, String fileName, int lineNumber, String fieldName) {
        super(message, fileName, lineNumber, fieldName);
    }
}
