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
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Basic Information
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email")
    private String email;

    @Column(name = "phone", nullable = false)
    private String phone;

    // Vehicle Information
    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false)
    @Builder.Default
    private VehicleTypeEnum vehicleType = VehicleTypeEnum.SEDAN;

    @Column(name = "vehicle_capacity")
    @Builder.Default
    private Integer vehicleCapacity = 4;

    // Medical Transport Capabilities
    @Column(name = "wheelchair_accessible", columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    private Boolean wheelchairAccessible = false;

    @Column(name = "stretcher_capable", columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    private Boolean stretcherCapable = false;

    @Column(name = "oxygen_equipped", columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    private Boolean oxygenEquipped = false;

    // Skills and Certifications
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "skills", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Boolean> skills = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "certifications", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> certifications = new ArrayList<>();

    // Availability and Limits
    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "max_daily_rides")
    @Builder.Default
    private Integer maxDailyRides = 8;

    @Column(name = "shift_start")
    private LocalTime shiftStart;

    @Column(name = "shift_end")
    private LocalTime shiftEnd;

    // Base Location
    @Column(name = "base_location", nullable = false)
    private String baseLocation;

    @Column(name = "base_lat", nullable = false)
    private Double baseLat;

    @Column(name = "base_lng", nullable = false)
    private Double baseLng;

    // License and Compliance
    @Column(name = "drivers_license_expiry")
    private LocalDate driversLicenseExpiry;

    @Column(name = "medical_transport_license_expiry")
    private LocalDate medicalTransportLicenseExpiry;

    @Column(name = "insurance_expiry")
    private LocalDate insuranceExpiry;

    @Column(name = "is_training_complete", columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    private Boolean isTrainingComplete = false;

    // Audit
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // !! REMOVED LAZY RELATIONSHIPS TO PREVENT LazyInitializationException !!
    // Instead, use repository queries when you need ride information

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