package com.mediroute.DTO;

public record RideUploadSummary(
        int uploaded,
        int returnCreated,
        int skippedBlank,
        int skippedCancelled,
        int skippedTime,
        int skippedRequired
) {}
