package com.mediroute.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Patient summary statistics")
public class PatientSummaryDTO {
    @Schema(description = "Patient ID")
    private Long patientId;

    @Schema(description = "Patient name")
    private String name;

    @Schema(description = "Total completed rides")
    private Long totalRides;

    @Schema(description = "On-time rides")
    private Long onTimeRides;

    @Schema(description = "On-time percentage")
    private Double onTimePercentage;

    @Schema(description = "Average ride distance")
    private Double averageDistance;

    @Schema(description = "Average ride duration")
    private Double averageDuration;

    @Schema(description = "Has special medical needs")
    private Boolean hasSpecialNeeds;
}
