package com.mediroute.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mediroute.entity.Driver;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * Main Driver DTO for all CRUD operations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Driver data transfer object")
public class DriverDTO {

    // Read-only fields (only populated on responses)
    @Schema(description = "Driver ID", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "Created timestamp", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @Schema(description = "Updated timestamp", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;

    // Basic Information (required for create)
    @NotBlank(message = "Name is required", groups = {Create.class})
    @Schema(description = "Driver full name", required = true)
    private String name;

    @Email(message = "Invalid email format")
    @Schema(description = "Driver email address")
    private String email;

    @NotBlank(message = "Phone is required", groups = {Create.class})
    @Pattern(regexp = "^[+]?[1-9]\\d{1,14}$", message = "Invalid phone number")
    @Schema(description = "Driver phone number", required = true)
    private String phone;

    // Vehicle Information
    @NotNull(message = "Vehicle type is required", groups = {Create.class})
    @Schema(description = "Vehicle type", required = true)
    private VehicleTypeEnum vehicleType;

    @Min(1)
    @Max(20)
    @Schema(description = "Vehicle passenger capacity", example = "4")
    private Integer vehicleCapacity;

    // Medical Transport Capabilities
    @Schema(description = "Vehicle is wheelchair accessible")
    private Boolean wheelchairAccessible;

    @Schema(description = "Vehicle can handle stretcher patients")
    private Boolean stretcherCapable;

    @Schema(description = "Vehicle has oxygen equipment")
    private Boolean oxygenEquipped;

    // Skills and Certifications
    @Schema(description = "Driver skills and capabilities")
    private Map<String, Boolean> skills;

    @Schema(description = "Driver certifications")
    private List<String> certifications;

    // Availability
    @Schema(description = "Whether driver is active")
    private Boolean active;

    @Schema(description = "Maximum rides per day", example = "8")
    @Min(1)
    @Max(20)
    private Integer maxDailyRides;

    @Schema(description = "Shift start time")
    private LocalTime shiftStart;

    @Schema(description = "Shift end time")
    private LocalTime shiftEnd;

    // Location
    @NotBlank(message = "Base location is required", groups = {Create.class})
    @Schema(description = "Base location address", required = true)
    private String baseLocation;

    @NotNull(message = "Base latitude is required", groups = {Create.class})
    @Schema(description = "Base location latitude", required = true)
    private Double baseLat;

    @NotNull(message = "Base longitude is required", groups = {Create.class})
    @Schema(description = "Base location longitude", required = true)
    private Double baseLng;

    // License and Compliance
    @Schema(description = "Driver's license expiry date")
    private LocalDate driversLicenseExpiry;

    @Schema(description = "Medical transport license expiry")
    private LocalDate medicalTransportLicenseExpiry;

    @Schema(description = "Insurance expiry date")
    private LocalDate insuranceExpiry;

    @Schema(description = "Training completion status")
    private Boolean isTrainingComplete;

    // Validation groups for create vs update
    public interface Create {
    }

    public interface Update {
    }

    // ========== Static Factory Methods ==========

    /**
     * Convert Driver entity to DTO
     */
    public static DriverDTO fromEntity(Driver driver) {
        if (driver == null) {
            return null;
        }

        return DriverDTO.builder()
                .id(driver.getId())
                .name(driver.getName())
                .email(driver.getEmail())
                .phone(driver.getPhone())
                .vehicleType(driver.getVehicleType())
                .vehicleCapacity(driver.getVehicleCapacity())
                .wheelchairAccessible(driver.getWheelchairAccessible())
                .stretcherCapable(driver.getStretcherCapable())
                .oxygenEquipped(driver.getOxygenEquipped())
                .skills(driver.getSkills())
                .certifications(driver.getCertifications())
                .active(driver.getActive())
                .maxDailyRides(driver.getMaxDailyRides())
                .shiftStart(driver.getShiftStart())
                .shiftEnd(driver.getShiftEnd())
                .baseLocation(driver.getBaseLocation())
                .baseLat(driver.getBaseLat())
                .baseLng(driver.getBaseLng())
                .driversLicenseExpiry(driver.getDriversLicenseExpiry())
                .medicalTransportLicenseExpiry(driver.getMedicalTransportLicenseExpiry())
                .insuranceExpiry(driver.getInsuranceExpiry())
                .isTrainingComplete(driver.getIsTrainingComplete())
                .createdAt(driver.getCreatedAt())
                .updatedAt(driver.getUpdatedAt())
                .build();
    }

    /**
     * Convert DTO to entity (for create/update operations)
     */
    public Driver toEntity() {
        return Driver.builder()
                .id(this.id)
                .name(this.name)
                .email(this.email)
                .phone(this.phone)
                .vehicleType(this.vehicleType)
                .vehicleCapacity(this.vehicleCapacity != null ? this.vehicleCapacity : 4)
                .wheelchairAccessible(this.wheelchairAccessible != null ? this.wheelchairAccessible : false)
                .stretcherCapable(this.stretcherCapable != null ? this.stretcherCapable : false)
                .oxygenEquipped(this.oxygenEquipped != null ? this.oxygenEquipped : false)
                .skills(this.skills != null ? this.skills : new java.util.HashMap<>())
                .certifications(this.certifications != null ? this.certifications : new java.util.ArrayList<>())
                .active(this.active != null ? this.active : true)
                .maxDailyRides(this.maxDailyRides != null ? this.maxDailyRides : 8)
                .shiftStart(this.shiftStart)
                .shiftEnd(this.shiftEnd)
                .baseLocation(this.baseLocation)
                .baseLat(this.baseLat)
                .baseLng(this.baseLng)
                .driversLicenseExpiry(this.driversLicenseExpiry)
                .medicalTransportLicenseExpiry(this.medicalTransportLicenseExpiry)
                .insuranceExpiry(this.insuranceExpiry)
                .isTrainingComplete(this.isTrainingComplete != null ? this.isTrainingComplete : false)
                .build();
    }

    /**
     * Update existing entity with DTO values (preserves null fields)
     */
    public void updateEntity(Driver driver) {
        if (this.name != null) driver.setName(this.name);
        if (this.email != null) driver.setEmail(this.email);
        if (this.phone != null) driver.setPhone(this.phone);
        if (this.vehicleType != null) driver.setVehicleType(this.vehicleType);
        if (this.vehicleCapacity != null) driver.setVehicleCapacity(this.vehicleCapacity);
        if (this.wheelchairAccessible != null) driver.setWheelchairAccessible(this.wheelchairAccessible);
        if (this.stretcherCapable != null) driver.setStretcherCapable(this.stretcherCapable);
        if (this.oxygenEquipped != null) driver.setOxygenEquipped(this.oxygenEquipped);
        if (this.skills != null) driver.setSkills(this.skills);
        if (this.certifications != null) driver.setCertifications(this.certifications);
        if (this.active != null) driver.setActive(this.active);
        if (this.maxDailyRides != null) driver.setMaxDailyRides(this.maxDailyRides);
        if (this.shiftStart != null) driver.setShiftStart(this.shiftStart);
        if (this.shiftEnd != null) driver.setShiftEnd(this.shiftEnd);
        if (this.baseLocation != null) driver.setBaseLocation(this.baseLocation);
        if (this.baseLat != null) driver.setBaseLat(this.baseLat);
        if (this.baseLng != null) driver.setBaseLng(this.baseLng);
        if (this.driversLicenseExpiry != null) driver.setDriversLicenseExpiry(this.driversLicenseExpiry);
        if (this.medicalTransportLicenseExpiry != null)
            driver.setMedicalTransportLicenseExpiry(this.medicalTransportLicenseExpiry);
        if (this.insuranceExpiry != null) driver.setInsuranceExpiry(this.insuranceExpiry);
        if (this.isTrainingComplete != null) driver.setIsTrainingComplete(this.isTrainingComplete);
    }
}