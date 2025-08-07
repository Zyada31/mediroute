package com.mediroute.dto;

public record RideUploadSummary(
        int uploaded,
        int returnCreated,
        int skippedBlank,
        int skippedCancelled,
        int skippedTime,
        int skippedRequired
) {}
