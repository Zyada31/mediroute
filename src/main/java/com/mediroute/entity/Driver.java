package com.mediroute.entity;

import com.mediroute.dto.VehicleTypeEnum;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

// Enhanced Driver Entity (extends your current one)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "drivers",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"name", "phone"})
        }
)
public class Driver {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = true, unique = false)
    private String email;

    private String phone;

    // Enhanced vehicle type options
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VehicleTypeEnum vehicleType = VehicleTypeEnum.SEDAN.SEDAN;

    // NEW: Vehicle capabilities for medical transport
    @Column(name = "wheelchair_accessible", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean wheelchairAccessible;

    @Column(name = "stretcher_capable", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean stretcherCapable;

    @Column(name = "oxygen_equipped", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean oxygenEquipped;

    @Column(name = "vehicle_capacity")
    private Integer vehicleCapacity;

    // Your existing fields
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "skills", columnDefinition = "jsonb", nullable = false)
    private Map<String, Boolean> skills;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "shift_start")
    private LocalDateTime shiftStart;

    @Column(name = "shift_end")
    private LocalDateTime shiftEnd;

    @OneToMany(mappedBy = "driver", fetch = FetchType.LAZY)
    private List<Ride> rides;

    // NEW: Split relationships for pickup vs dropoff
    @OneToMany(mappedBy = "pickupDriver")
    private List<Ride> pickupRides;

    @OneToMany(mappedBy = "dropoffDriver")
    private List<Ride> dropoffRides;

    @Column(name = "max_daily_rides")
    private Integer maxDailyRides = 8;

    @Column(name = "base_location", nullable = false)
    private String baseLocation;

    @Column(name = "base_lat", nullable = false)
    private double baseLat;

    @Column(name = "base_lng", nullable = false)
    private double baseLng;

    // NEW: Driver certifications and compliance
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "certifications", columnDefinition = "jsonb")
    private List<String> certifications;

    @Column(name = "drivers_license_expiry")
    private LocalDate driversLicenseExpiry;

    @Column(name = "medical_transport_license_expiry")
    private LocalDate medicalTransportLicenseExpiry;

    @Column(name = "insurance_expiry")
    private LocalDate insuranceExpiry;

    @Column(name = "is_training_complete", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isTrainingComplete;

    // Helper methods for medical transport
    public boolean canHandlePatient(Patient patient) {
        // Check vehicle type compatibility
        if (Boolean.TRUE.equals(patient.getRequiresWheelchair()) &&
                !Boolean.TRUE.equals(this.wheelchairAccessible)) {
            return false;
        }

        if (Boolean.TRUE.equals(patient.getRequiresStretcher()) &&
                !Boolean.TRUE.equals(this.stretcherCapable)) {
            return false;
        }

        if (Boolean.TRUE.equals(patient.getRequiresOxygen()) &&
                !Boolean.TRUE.equals(this.oxygenEquipped)) {
            return false;
        }

        return true;
    }

    public boolean isAvailableAt(LocalDateTime dateTime) {
        if (!active) return false;

        // Check if within shift hours
        if (shiftStart != null && shiftEnd != null) {
            LocalTime requestTime = dateTime.toLocalTime();
            LocalTime shiftStartTime = shiftStart.toLocalTime();
            LocalTime shiftEndTime = shiftEnd.toLocalTime();

            return requestTime.isAfter(shiftStartTime) && requestTime.isBefore(shiftEndTime);
        }

        return true;
    }

    public boolean needsLicenseRenewal(int daysAhead) {
        LocalDate checkDate = LocalDate.now().plusDays(daysAhead);
        return (driversLicenseExpiry != null && driversLicenseExpiry.isBefore(checkDate)) ||
                (medicalTransportLicenseExpiry != null && medicalTransportLicenseExpiry.isBefore(checkDate)) ||
                (insuranceExpiry != null && insuranceExpiry.isBefore(checkDate));
    }

}

