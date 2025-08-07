package com.mediroute.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Ride statistics summary")
public class RideStatistics {
    @Schema(description = "Statistics date")
    private LocalDate date;

    @Schema(description = "Total rides")
    private Integer totalRides;

    @Schema(description = "Assigned rides")
    private Integer assignedRides;

    @Schema(description = "Unassigned rides")
    private Integer unassignedRides;

    @Schema(description = "Emergency rides")
    private Integer emergencyRides;

    @Schema(description = "Wheelchair rides")
    private Integer wheelchairRides;

    @Schema(description = "Round trip rides")
    private Integer roundTripRides;

    @Schema(description = "Assignment rate percentage")
    private Double assignmentRate;
}
