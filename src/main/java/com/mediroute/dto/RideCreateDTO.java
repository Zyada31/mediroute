package com.mediroute.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Ride creation request")
public class RideCreateDTO {

    @NotNull(message = "Patient ID is required")
    @Schema(description = "Patient ID", required = true)
    private Long patientId;

    @NotBlank(message = "Pickup location is required")
    @Schema(description = "Pickup location address", required = true)
    private String pickupLocation;

    @NotBlank(message = "Dropoff location is required")
    @Schema(description = "Dropoff location address", required = true)
    private String dropoffLocation;

    @NotNull(message = "Pickup time is required")
    @Future(message = "Pickup time must be in the future")
    @Schema(description = "Scheduled pickup time", required = true)
    private LocalDateTime pickupTime;

    @Schema(description = "Scheduled dropoff time")
    private LocalDateTime dropoffTime;

    @Schema(description = "Appointment duration in minutes")
    private Integer appointmentDuration;

    @Schema(description = "Type of ride")
    private RideType rideType = RideType.ONE_WAY;

    @Schema(description = "Priority level")
    private Priority priority = Priority.ROUTINE;

    @Schema(description = "Required vehicle type")
    private String requiredVehicleType;

    @Schema(description = "Required driver skills")
    private List<String> requiredSkills;

    @Schema(description = "Is this a round trip")
    private Boolean isRoundTrip = false;
}
