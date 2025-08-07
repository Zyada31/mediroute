package com.mediroute.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Driver statistics summary")
public class DriverStatistics {
    @Schema(description = "Total active drivers")
    private Long totalActiveDrivers;

    @Schema(description = "Wheelchair accessible drivers")
    private Integer wheelchairAccessibleCount;

    @Schema(description = "Stretcher capable drivers")
    private Integer stretcherCapableCount;

    @Schema(description = "Oxygen equipped drivers")
    private Integer oxygenEquippedCount;

    @Schema(description = "Vehicle type distribution")
    private List<VehicleTypeCount> vehicleTypeDistribution;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Vehicle type count")
    public static class VehicleTypeCount {
        @Schema(description = "Vehicle type name")
        private String vehicleType;

        @Schema(description = "Number of drivers with this vehicle type")
        private Integer count;
    }
}
