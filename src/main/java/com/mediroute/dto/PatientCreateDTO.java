// ============================================================================
// MISSING DTOs - Required for Compilation
// ============================================================================

// 1. Ride DTOs
package com.mediroute.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Patient creation request")
public class PatientCreateDTO {

    @NotBlank(message = "Name is required")
    @Schema(description = "Patient full name", required = true, example = "John Doe")
    private String name;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[+]?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    @Schema(description = "Patient phone number", required = true, example = "+1234567890")
    private String phone;

    @Email(message = "Invalid email format")
    @Schema(description = "Patient email address", example = "john.doe@email.com")
    private String email;

    @Schema(description = "Emergency contact name", example = "Jane Doe")
    private String emergencyContactName;

    @Schema(description = "Emergency contact phone", example = "+1987654321")
    private String emergencyContactPhone;

    @Schema(description = "Requires wheelchair accessible vehicle")
    private Boolean requiresWheelchair = false;

    @Schema(description = "Requires stretcher capable vehicle")
    private Boolean requiresStretcher = false;

    @Schema(description = "Requires oxygen equipped vehicle")
    private Boolean requiresOxygen = false;

    @Schema(description = "Patient mobility level")
    private MobilityLevel mobilityLevel;

    @Schema(description = "Insurance provider name", example = "Blue Cross")
    private String insuranceProvider;

    @Schema(description = "Insurance ID number", example = "BC123456789")
    private String insuranceId;

    @Schema(description = "Date of birth")
    private LocalDate dateOfBirth;
}









