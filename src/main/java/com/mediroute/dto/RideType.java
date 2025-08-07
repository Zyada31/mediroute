package com.mediroute.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Type of ride")
public enum RideType {
    @Schema(description = "One-way trip")
    ONE_WAY,

    @Schema(description = "Round trip with wait time")
    ROUND_TRIP,

    @Schema(description = "Recurring ride pattern")
    RECURRING
}