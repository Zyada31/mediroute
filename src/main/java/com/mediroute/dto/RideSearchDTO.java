package com.mediroute.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Ride search criteria")
public class RideSearchDTO {
    @Schema(description = "Start date for search")
    private LocalDate startDate;

    @Schema(description = "End date for search")
    private LocalDate endDate;

    @Schema(description = "Ride status filter")
    private RideStatus status;

    @Schema(description = "Priority filter")
    private Priority priority;

    @Schema(description = "Vehicle type filter")
    private String vehicleType;

    @Schema(description = "Driver ID filter")
    private Long driverId;

    @Schema(description = "Patient ID filter")
    private Long patientId;
}
