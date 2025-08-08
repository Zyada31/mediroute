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
public class DriverStatisticsDTO {
    @Schema(description = "Total active drivers")
    private Long totalActiveDrivers;

    @Schema(description = "Vehicle type counts")
    private List<VehicleTypeCount> vehicleTypeCounts;

    @Schema(description = "Wheelchair accessible drivers")
    private Integer wheelchairAccessibleCount;

    @Schema(description = "Stretcher capable drivers")
    private Integer stretcherCapableCount;

    @Schema(description = "Oxygen equipped drivers")
    private Integer oxygenEquippedCount;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleTypeCount {
        private String vehicleType;
        private Integer count;
    }
}
