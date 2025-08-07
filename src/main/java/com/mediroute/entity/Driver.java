package com.mediroute.entity;

import com.mediroute.dto.VehicleTypeEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "drivers",
        uniqueConstraints = @UniqueConstraint(columnNames = {"name", "phone"}),
        indexes = {
                @Index(name = "idx_driver_active", columnList = "active"),
                @Index(name = "idx_driver_vehicle_type", columnList = "vehicle_type"),
                @Index(name = "idx_driver_location", columnList = "base_lat, base_lng")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@Schema(description = "Driver entity for medical transport")
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique driver identifier")
    private Long id;

    // Basic Information
    @Column(name = "name", nullable = false)
    @Schema(description = "Driver full name", required = true)
    private String name;

    @Column(name = "email")
    @Schema(description = "Driver email address")
    private String email;

    @Column(name = "phone", nullable = false)
    @Schema(description = "Driver phone number", required = true)
    private String phone;

    // Vehicle Information
    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false)
    @Builder.Default
    @Schema(description = "Type of vehicle")
    private VehicleTypeEnum vehicleType = VehicleTypeEnum.SEDAN;

    @Column(name = "vehicle_capacity")
    @Builder.Default
    @Schema(description = "Vehicle passenger capacity")
    private Integer vehicleCapacity = 4;

    // Medical Transport Capabilities
    @Column(name = "wheelchair_accessible", columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    @Schema(description = "Vehicle is wheelchair accessible")
    private Boolean wheelchairAccessible = false;

    @Column(name = "stretcher_capable", columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    @Schema(description = "Vehicle can handle stretcher patients")
    private Boolean stretcherCapable = false;

    @Column(name = "oxygen_equipped", columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    @Schema(description = "Vehicle has oxygen equipment")
    private Boolean oxygenEquipped = false;

    // Skills and Certifications
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "skills", columnDefinition = "jsonb")
    @Builder.Default
    @Schema(description = "Driver skills and capabilities")
    private Map<String, Boolean> skills = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "certifications", columnDefinition = "jsonb")
    @Builder.Default
    @Schema(description = "Driver certifications")
    private List<String> certifications = new ArrayList<>();

    // Availability and Limits
    @Column(name = "active", nullable = false)
    @Builder.Default
    @Schema(description = "Whether driver is active")
    private Boolean active = true;

    @Column(name = "max_daily_rides")
    @Builder.Default
    @Schema(description = "Maximum rides per day")
    private Integer maxDailyRides = 8;

    @Column(name = "shift_start")
    @Schema(description = "Shift start time")
    private LocalTime shiftStart;

    @Column(name = "shift_end")
    @Schema(description = "Shift end time")
    private LocalTime shiftEnd;

    // Base Location
    @Column(name = "base_location", nullable = false)
    @Schema(description = "Base location address")
    private String baseLocation;

    @Column(name = "base_lat", nullable = false)
    @Schema(description = "Base location latitude")
    private Double baseLat;

    @Column(name = "base_lng", nullable = false)
    @Schema(description = "Base location longitude")
    private Double baseLng;

    // License and Compliance
    @Column(name = "drivers_license_expiry")
    @Schema(description = "Driver's license expiry date")
    private LocalDate driversLicenseExpiry;

    @Column(name = "medical_transport_license_expiry")
    @Schema(description = "Medical transport license expiry")
    private LocalDate medicalTransportLicenseExpiry;

    @Column(name = "insurance_expiry")
    @Schema(description = "Insurance expiry date")
    private LocalDate insuranceExpiry;

    @Column(name = "is_training_complete", columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    @Schema(description = "Whether training is complete")
    private Boolean isTrainingComplete = false;

    // Audit
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationships
    @OneToMany(mappedBy = "pickupDriver", fetch = FetchType.LAZY)
    private List<Ride> pickupRides = new ArrayList<>();

    @OneToMany(mappedBy = "dropoffDriver", fetch = FetchType.LAZY)
    private List<Ride> dropoffRides = new ArrayList<>();

    // Business Methods
    public boolean canHandlePatient(Patient patient) {
        if (patient == null) return true;

        return !(Boolean.TRUE.equals(patient.getRequiresWheelchair()) && !Boolean.TRUE.equals(this.wheelchairAccessible)) &&
                !(Boolean.TRUE.equals(patient.getRequiresStretcher()) && !Boolean.TRUE.equals(this.stretcherCapable)) &&
                !(Boolean.TRUE.equals(patient.getRequiresOxygen()) && !Boolean.TRUE.equals(this.oxygenEquipped));
    }

    public boolean isAvailableAt(LocalDateTime dateTime) {
        if (!active || !Boolean.TRUE.equals(isTrainingComplete)) return false;

        if (shiftStart != null && shiftEnd != null) {
            LocalTime requestTime = dateTime.toLocalTime();
            return !requestTime.isBefore(shiftStart) && !requestTime.isAfter(shiftEnd);
        }

        return true;
    }

    public boolean needsLicenseRenewal(int daysAhead) {
        LocalDate checkDate = LocalDate.now().plusDays(daysAhead);
        return (driversLicenseExpiry != null && driversLicenseExpiry.isBefore(checkDate)) ||
                (medicalTransportLicenseExpiry != null && medicalTransportLicenseExpiry.isBefore(checkDate)) ||
                (insuranceExpiry != null && insuranceExpiry.isBefore(checkDate));
    }

    public void addSkill(String skill) {
        if (this.skills == null) {
            this.skills = new HashMap<>();
        }
        this.skills.put(skill, true);
    }

    public void addCertification(String certification) {
        if (this.certifications == null) {
            this.certifications = new ArrayList<>();
        }
        if (!this.certifications.contains(certification)) {
            this.certifications.add(certification);
        }
    }
}