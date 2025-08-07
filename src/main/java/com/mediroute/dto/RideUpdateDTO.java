package com.mediroute.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Ride update request")
public class RideUpdateDTO {
    @Schema(description = "Scheduled pickup time")
    private LocalDateTime pickupTime;

    @Schema(description = "Scheduled dropoff time")
    private LocalDateTime dropoffTime;

    @Schema(description = "Ride status")
    private RideStatus status;

    @Schema(description = "Priority level")
    private Priority priority;

    @Schema(description = "Appointment duration in minutes")
    private Integer appointmentDuration;

    @Schema(description = "Required vehicle type")
    private String requiredVehicleType;
}
