package com.mediroute.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Patient update request")
public class PatientUpdateDTO {
    @Schema(description = "Patient full name")
    private String name;

    @Pattern(regexp = "^[+]?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    @Schema(description = "Patient phone number")
    private String phone;

    @Email(message = "Invalid email format")
    @Schema(description = "Patient email address")
    private String email;

    @Schema(description = "Emergency contact name")
    private String emergencyContactName;

    @Schema(description = "Emergency contact phone")
    private String emergencyContactPhone;

    @Schema(description = "Requires wheelchair accessible vehicle")
    private Boolean requiresWheelchair;

    @Schema(description = "Requires stretcher capable vehicle")
    private Boolean requiresStretcher;

    @Schema(description = "Requires oxygen equipped vehicle")
    private Boolean requiresOxygen;

    @Schema(description = "Patient mobility level")
    private MobilityLevel mobilityLevel;

    @Schema(description = "Insurance provider name")
    private String insuranceProvider;

    @Schema(description = "Insurance ID number")
    private String insuranceId;
}
