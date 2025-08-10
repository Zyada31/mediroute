// ============================================================================
// MISSING DTOs - Required for Compilation
// ============================================================================

// 1. Ride DTOs
package com.mediroute.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//@Schema(description = "Patient creation request")
//public class PatientCreateDTO {
//
//    @NotBlank(message = "Name is required")
//    @Schema(description = "Patient full name", required = true, example = "John Doe")
//    private String name;
//
//    @NotBlank(message = "Phone is required")
//    @Pattern(regexp = "^[+]?[1-9]\\d{1,14}$", message = "Invalid phone number format")
//    @Schema(description = "Patient phone number", required = true, example = "+1234567890")
//    private String phone;
//
//    @Email(message = "Invalid email format")
//    @Schema(description = "Patient email address", example = "john.doe@email.com")
//    private String email;
//
//    @Schema(description = "Emergency contact name", example = "Jane Doe")
//    private String emergencyContactName;
//
//    @Schema(description = "Emergency contact phone", example = "+1987654321")
//    private String emergencyContactPhone;
//
//    @Schema(description = "Requires wheelchair accessible vehicle")
//    private Boolean requiresWheelchair = false;
//
//    @Schema(description = "Requires stretcher capable vehicle")
//    private Boolean requiresStretcher = false;
//
//    @Schema(description = "Requires oxygen equipped vehicle")
//    private Boolean requiresOxygen = false;
//
//    @Schema(description = "Patient mobility level")
//    private MobilityLevel mobilityLevel;
//
//    @Schema(description = "Insurance provider name", example = "Blue Cross")
//    private String insuranceProvider;
//
//    @Schema(description = "Insurance ID number", example = "BC123456789")
//    private String insuranceId;
//
//    @Schema(description = "Date of birth")
//    private LocalDate dateOfBirth;
//}
//
/**
 * Patient DTO for all CRUD operations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Patient data transfer object")
public class PatientDTO {

    @Schema(description = "Patient ID", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank(message = "Name is required", groups = {DriverDTO.Create.class})
    @Schema(description = "Patient full name", required = true)
    private String name;

    @NotBlank(message = "Phone is required", groups = {DriverDTO.Create.class})
    @Pattern(regexp = "^[+]?[1-9]\\d{1,14}$", message = "Invalid phone number")
    @Schema(description = "Patient phone number", required = true)
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

    @Schema(description = "Date of birth")
    private LocalDate dateOfBirth;

    @Schema(description = "Default pickup location")
    private LocationDTO defaultPickupLocation;

    @Schema(description = "Default dropoff location")
    private LocationDTO defaultDropoffLocation;

    @Schema(description = "Medical conditions")
    private List<String> medicalConditions;

    @Schema(description = "Special needs")
    private Map<String, Object> specialNeeds;

    @Schema(description = "Active status")
    private Boolean isActive;

    @Schema(description = "Created timestamp", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @Schema(description = "Updated timestamp", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;

    public String getRequiredVehicleType() {
        if (Boolean.TRUE.equals(requiresStretcher)) return "STRETCHER_VAN";
        if (Boolean.TRUE.equals(requiresWheelchair)) return "WHEELCHAIR_VAN";
        if (Boolean.TRUE.equals(requiresOxygen)) return "WHEELCHAIR_VAN";
        return "SEDAN";
    }
}
