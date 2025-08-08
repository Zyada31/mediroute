package com.mediroute.dto;

import com.mediroute.entity.Driver;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Driver details for API responses")
public class DriverDetailDTO {

    @Schema(description = "Driver unique identifier")
    private Long id;

    @Schema(description = "Driver full name")
    private String name;

    @Schema(description = "Driver email address")
    private String email;

    @Schema(description = "Driver phone number")
    private String phone;

    @Schema(description = "Vehicle type")
    private String vehicleType;

    @Schema(description = "Vehicle passenger capacity")
    private Integer vehicleCapacity;

    @Schema(description = "Vehicle is wheelchair accessible")
    private Boolean wheelchairAccessible;

    @Schema(description = "Vehicle can handle stretcher patients")
    private Boolean stretcherCapable;

    @Schema(description = "Vehicle has oxygen equipment")
    private Boolean oxygenEquipped;

    @Schema(description = "Driver skills and capabilities")
    private Map<String, Boolean> skills;

    @Schema(description = "Driver certifications")
    private List<String> certifications;

    @Schema(description = "Whether driver is active")
    private Boolean active;

    @Schema(description = "Maximum rides per day")
    private Integer maxDailyRides;

    @Schema(description = "Shift start time")
    private LocalTime shiftStart;

    @Schema(description = "Shift end time")
    private LocalTime shiftEnd;

    @Schema(description = "Base location address")
    private String baseLocation;

    @Schema(description = "Base location latitude")
    private Double baseLat;

    @Schema(description = "Base location longitude")
    private Double baseLng;

    @Schema(description = "Driver's license expiry date")
    private LocalDate driversLicenseExpiry;

    @Schema(description = "Medical transport license expiry")
    private LocalDate medicalTransportLicenseExpiry;

    @Schema(description = "Insurance expiry date")
    private LocalDate insuranceExpiry;

    @Schema(description = "Whether training is complete")
    private Boolean isTrainingComplete;

    @Schema(description = "When driver was created")
    private LocalDateTime createdAt;

    @Schema(description = "When driver was last updated")
    private LocalDateTime updatedAt;

    // ========== CONVERSION METHODS ==========

    /**
     * Convert Driver entity to DTO safely (no lazy loading issues)
     */
    public static DriverDetailDTO fromEntity(Driver driver) {
        if (driver == null) {
            return null;
        }

        return DriverDetailDTO.builder()
                .id(driver.getId())
                .name(driver.getName())
                .email(driver.getEmail())
                .phone(driver.getPhone())
                .vehicleType(driver.getVehicleType() != null ? driver.getVehicleType().name() : null)
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
     * Create a summary version with only essential fields
     */
    public static DriverDetailDTO summaryFromEntity(Driver driver) {
        if (driver == null) {
            return null;
        }

        return DriverDetailDTO.builder()
                .id(driver.getId())
                .name(driver.getName())
                .phone(driver.getPhone())
                .vehicleType(driver.getVehicleType() != null ? driver.getVehicleType().name() : null)
                .wheelchairAccessible(driver.getWheelchairAccessible())
                .stretcherCapable(driver.getStretcherCapable())
                .oxygenEquipped(driver.getOxygenEquipped())
                .active(driver.getActive())
                .isTrainingComplete(driver.getIsTrainingComplete())
                .baseLocation(driver.getBaseLocation())
                .build();
    }

    // ========== BUSINESS LOGIC METHODS ==========

    public boolean canHandleMedicalTransport() {
        return Boolean.TRUE.equals(isTrainingComplete) &&
                (Boolean.TRUE.equals(wheelchairAccessible) ||
                        Boolean.TRUE.equals(stretcherCapable) ||
                        Boolean.TRUE.equals(oxygenEquipped));
    }

    public boolean isQualified() {
        return Boolean.TRUE.equals(active) &&
                Boolean.TRUE.equals(isTrainingComplete) &&
                !hasExpiredLicenses();
    }

    public boolean hasExpiredLicenses() {
        LocalDate today = LocalDate.now();
        return (driversLicenseExpiry != null && driversLicenseExpiry.isBefore(today)) ||
                (medicalTransportLicenseExpiry != null && medicalTransportLicenseExpiry.isBefore(today)) ||
                (insuranceExpiry != null && insuranceExpiry.isBefore(today));
    }

    public boolean needsLicenseRenewal(int daysAhead) {
        LocalDate checkDate = LocalDate.now().plusDays(daysAhead);
        return (driversLicenseExpiry != null && driversLicenseExpiry.isBefore(checkDate)) ||
                (medicalTransportLicenseExpiry != null && medicalTransportLicenseExpiry.isBefore(checkDate)) ||
                (insuranceExpiry != null && insuranceExpiry.isBefore(checkDate));
    }

    public String getVehicleCapabilitiesSummary() {
        List<String> capabilities = new java.util.ArrayList<>();

        if (Boolean.TRUE.equals(wheelchairAccessible)) {
            capabilities.add("Wheelchair");
        }
        if (Boolean.TRUE.equals(stretcherCapable)) {
            capabilities.add("Stretcher");
        }
        if (Boolean.TRUE.equals(oxygenEquipped)) {
            capabilities.add("Oxygen");
        }

        return capabilities.isEmpty() ? "Standard" : String.join(", ", capabilities);
    }

    public String getAvailabilityStatus() {
        if (!Boolean.TRUE.equals(active)) {
            return "INACTIVE";
        }
        if (!Boolean.TRUE.equals(isTrainingComplete)) {
            return "TRAINING_PENDING";
        }
        if (hasExpiredLicenses()) {
            return "LICENSE_EXPIRED";
        }
        if (needsLicenseRenewal(30)) {
            return "LICENSE_EXPIRING";
        }
        return "AVAILABLE";
    }
}
