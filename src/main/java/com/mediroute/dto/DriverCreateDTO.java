// 3. Driver DTOs
package com.mediroute.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Driver creation request")
public class DriverCreateDTO {

    @NotBlank(message = "Name is required")
    @Schema(description = "Driver full name", required = true)
    private String name;

    @Email(message = "Invalid email format")
    @Schema(description = "Driver email address")
    private String email;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[+]?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    @Schema(description = "Driver phone number", required = true)
    private String phone;

    @NotNull(message = "Vehicle type is required")
    @Schema(description = "Vehicle type", required = true)
    private VehicleTypeEnum vehicleType;

    @Schema(description = "Vehicle passenger capacity", example = "4")
    private Integer vehicleCapacity = 4;

    @Schema(description = "Vehicle is wheelchair accessible")
    private Boolean wheelchairAccessible = false;

    @Schema(description = "Vehicle can handle stretcher patients")
    private Boolean stretcherCapable = false;

    @Schema(description = "Vehicle has oxygen equipment")
    private Boolean oxygenEquipped = false;

    @Schema(description = "Maximum rides per day", example = "8")
    private Integer maxDailyRides = 8;

    @NotBlank(message = "Base location is required")
    @Schema(description = "Base location address", required = true)
    private String baseLocation;

    @NotNull(message = "Base latitude is required")
    @Schema(description = "Base location latitude", required = true)
    private Double baseLat;

    @NotNull(message = "Base longitude is required")
    @Schema(description = "Base location longitude", required = true)
    private Double baseLng;

    @Schema(description = "Shift start time")
    private LocalTime shiftStart;

    @Schema(description = "Shift end time")
    private LocalTime shiftEnd;

    @Schema(description = "Driver's license expiry date")
    private LocalDate driversLicenseExpiry;

    @Schema(description = "Medical transport license expiry")
    private LocalDate medicalTransportLicenseExpiry;

    @Schema(description = "Insurance expiry date")
    private LocalDate insuranceExpiry;

    @Schema(description = "Training completion status")
    private Boolean isTrainingComplete = false;
}
