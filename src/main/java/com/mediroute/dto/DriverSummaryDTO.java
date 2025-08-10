package com.mediroute.dto;

import com.mediroute.entity.Driver;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List; /**
 * Lightweight Driver DTO for lists and summaries
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Driver summary for list views")
public class DriverSummaryDTO {
    @Schema(description = "Driver ID")
    private Long id;

    @Schema(description = "Driver name")
    private String name;

    @Schema(description = "Phone number")
    private String phone;

    @Schema(description = "Vehicle type")
    private String vehicleType;

    @Schema(description = "Medical capabilities summary")
    private String capabilities;

    @Schema(description = "Active status")
    private Boolean active;

    @Schema(description = "Today's assigned rides")
    private Integer todaysRideCount;

    @Schema(description = "Utilization percentage")
    private Double utilizationRate;

    public static DriverSummaryDTO fromDriver(Driver driver) {
        return DriverSummaryDTO.builder()
                .id(driver.getId())
                .name(driver.getName())
                .phone(driver.getPhone())
                .vehicleType(driver.getVehicleType().name())
                .capabilities(getCapabilitiesString(driver))
                .active(driver.getActive())
                .build();
    }

    private static String getCapabilitiesString(Driver driver) {
        List<String> caps = new ArrayList<>();
        if (Boolean.TRUE.equals(driver.getWheelchairAccessible())) caps.add("WC");
        if (Boolean.TRUE.equals(driver.getStretcherCapable())) caps.add("ST");
        if (Boolean.TRUE.equals(driver.getOxygenEquipped())) caps.add("O2");
        return caps.isEmpty() ? "Standard" : String.join("/", caps);
    }
}
