package com.mediroute.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ride priority levels")
public enum Priority {
    @Schema(description = "Emergency priority - immediate attention required")
    EMERGENCY,

    @Schema(description = "Urgent priority - needs quick attention")
    URGENT,

    @Schema(description = "Routine priority - normal scheduling")
    ROUTINE
}