package com.mediroute.dto;

import com.mediroute.dto.VehicleTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Driver update request")
public class DriverUpdateDTO {
    @Schema(description = "Driver full name")
    private String name;

    @Email(message = "Invalid email format")
    @Schema(description = "Driver email address")
    private String email;

    @Pattern(regexp = "^[+]?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    @Schema(description = "Driver phone number")
    private String phone;

    @Schema(description = "Vehicle type")
    private VehicleTypeEnum vehicleType;

    @Schema(description = "Vehicle is wheelchair accessible")
    private Boolean wheelchairAccessible;

    @Schema(description = "Vehicle can handle stretcher patients")
    private Boolean stretcherCapable;

    @Schema(description = "Vehicle has oxygen equipment")
    private Boolean oxygenEquipped;

    @Schema(description = "Shift start time")
    private LocalTime shiftStart;

    @Schema(description = "Shift end time")
    private LocalTime shiftEnd;

    @Schema(description = "Training completion status")
    private Boolean isTrainingComplete;

    @Schema(description = "Driver active status")
    private Boolean active;
}
