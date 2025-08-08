package com.mediroute.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Result DTO for bulk ride upload operations
 */
public class BulkRideUploadResultDto {

    private int totalProcessed = 0;
    private int patientsCreated = 0;
    private int patientsUpdated = 0;
    private int ridesCreated = 0;
    private int ridesSkipped = 0;
    private int errorCount = 0;
    private List<ProcessingError> errors = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    // Constructors
    public BulkRideUploadResultDto() {}

    // Utility methods
    public void incrementTotalProcessed() {
        this.totalProcessed++;
    }

    public void incrementPatientsCreated() {
        this.patientsCreated++;
    }

    public void incrementPatientsUpdated() {
        this.patientsUpdated++;
    }

    public void incrementRidesCreated() {
        this.ridesCreated++;
    }

    public void incrementRidesSkipped() {
        this.ridesSkipped++;
    }

    public void addError(int lineNumber, String message) {
        this.errors.add(new ProcessingError(lineNumber, message));
        this.errorCount++;
    }

    public void addWarning(String message) {
        this.warnings.add(message);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    // Getters and Setters
    public int getTotalProcessed() { return totalProcessed; }
    public void setTotalProcessed(int totalProcessed) { this.totalProcessed = totalProcessed; }

    public int getPatientsCreated() { return patientsCreated; }
    public void setPatientsCreated(int patientsCreated) { this.patientsCreated = patientsCreated; }

    public int getPatientsUpdated() { return patientsUpdated; }
    public void setPatientsUpdated(int patientsUpdated) { this.patientsUpdated = patientsUpdated; }

    public int getRidesCreated() { return ridesCreated; }
    public void setRidesCreated(int ridesCreated) { this.ridesCreated = ridesCreated; }

    public int getRidesSkipped() { return ridesSkipped; }
    public void setRidesSkipped(int ridesSkipped) { this.ridesSkipped = ridesSkipped; }

    public int getErrorCount() { return errorCount; }
    public void setErrorCount(int errorCount) { this.errorCount = errorCount; }

    public List<ProcessingError> getErrors() { return errors; }
    public void setErrors(List<ProcessingError> errors) { this.errors = errors != null ? errors : new ArrayList<>(); }

    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings != null ? warnings : new ArrayList<>(); }

    /**
     * Processing error details
     */
    public static class ProcessingError {
        private int lineNumber;
        private String message;
        private String field;
        private String value;

        public ProcessingError() {}

        public ProcessingError(int lineNumber, String message) {
            this.lineNumber = lineNumber;
            this.message = message;
        }

        public ProcessingError(int lineNumber, String message, String field, String value) {
            this.lineNumber = lineNumber;
            this.message = message;
            this.field = field;
            this.value = value;
        }

        // Getters and Setters
        public int getLineNumber() { return lineNumber; }
        public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getField() { return field; }
        public void setField(String field) { this.field = field; }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }

        @Override
        public String toString() {
            return String.format("Line %d: %s%s%s",
                    lineNumber,
                    message,
                    field != null ? " (Field: " + field + ")" : "",
                    value != null ? " (Value: " + value + ")" : "");
        }
    }

    @Override
    public String toString() {
        return String.format(
                "BulkRideUploadResult{processed=%d, patientsCreated=%d, patientsUpdated=%d, ridesCreated=%d, ridesSkipped=%d, errors=%d, warnings=%d}",
                totalProcessed, patientsCreated, patientsUpdated, ridesCreated, ridesSkipped, errorCount, warnings.size()
        );
    }
}