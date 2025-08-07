package com.mediroute.dto;


import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Patient mobility levels")
public enum MobilityLevel {
    @Schema(description = "Patient can walk independently")
    INDEPENDENT,

    @Schema(description = "Patient needs assistance but can walk")
    ASSISTED,

    @Schema(description = "Patient requires wheelchair")
    WHEELCHAIR,

    @Schema(description = "Patient requires stretcher/gurney")
    STRETCHER
}