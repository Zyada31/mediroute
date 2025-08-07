// 5. DriverRideSummary DTO (if missing)
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
@Schema(description = "Driver ride assignment summary")
public class DriverRideSummary {
    @Schema(description = "Driver ID")
    private Long driverId;

    @Schema(description = "Driver name")
    private String driverName;

    @Schema(description = "Summary date")
    private LocalDate date;

    @Schema(description = "Total assigned rides")
    private Integer totalRides;

    @Schema(description = "Completed rides")
    private Integer completedRides;

    @Schema(description = "Pending rides")
    private Integer pendingRides;

    @Schema(description = "Cancelled rides")
    private Integer cancelledRides;

    @Schema(description = "List of ride IDs assigned to this driver")
    private List<Long> rideIds;

    @Schema(description = "Total distance for all rides")
    private Double totalDistance;

    @Schema(description = "Vehicle type")
    private String vehicleType;

    @Schema(description = "Whether driver handles medical transport")
    private Boolean medicalTransportCapable;
}