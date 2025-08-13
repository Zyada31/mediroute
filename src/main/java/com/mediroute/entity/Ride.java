package com.mediroute.entity;

import com.mediroute.dto.Priority;
import com.mediroute.dto.RideStatus;
import com.mediroute.dto.RideType;
import com.mediroute.entity.embeddable.Location;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rides", indexes = {
        @Index(name = "idx_ride_pickup_time", columnList = "pickup_time"),
        @Index(name = "idx_ride_status", columnList = "status"),
        @Index(name = "idx_ride_priority", columnList = "priority"),
        @Index(name = "idx_ride_batch", columnList = "optimization_batch_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Ride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_id")
    private Long orgId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pickup_driver_id")
    private Driver pickupDriver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dropoff_driver_id")
    private Driver dropoffDriver;

    // Backward compatibility - keep this for existing code
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    // Location Information
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "address", column = @Column(name = "pickup_address")),
            @AttributeOverride(name = "latitude", column = @Column(name = "pickup_lat")),
            @AttributeOverride(name = "longitude", column = @Column(name = "pickup_lng"))
    })
    private Location pickupLocation;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "address", column = @Column(name = "dropoff_address")),
            @AttributeOverride(name = "latitude", column = @Column(name = "dropoff_lat")),
            @AttributeOverride(name = "longitude", column = @Column(name = "dropoff_lng"))
    })
    private Location dropoffLocation;

    // Legacy location fields for backward compatibility
    @Column(name = "pickup_location")
    private String pickupLocationString;

    @Column(name = "dropoff_location")
    private String dropoffLocationString;

    // Timing Information
    @Column(name = "pickup_time", nullable = false)
    private LocalDateTime pickupTime;

    @Column(name = "dropoff_time")
    private LocalDateTime dropoffTime;

    @Column(name = "pickup_window_start")
    private LocalDateTime pickupWindowStart;

    @Column(name = "pickup_window_end")
    private LocalDateTime pickupWindowEnd;

    @Column(name = "dropoff_window_start")
    private LocalDateTime dropoffWindowStart;

    @Column(name = "dropoff_window_end")
    private LocalDateTime dropoffWindowEnd;

    @Column(name = "appointment_duration")
    private Integer appointmentDuration;

    // Ride Classification
    @Enumerated(EnumType.STRING)
    @Column(name = "ride_type")
    private RideType rideType;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority")
    @Builder.Default
    private Priority priority = Priority.ROUTINE;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private RideStatus status = RideStatus.SCHEDULED;

    // Vehicle and Requirements
    @Column(name = "required_vehicle_type")
    private String requiredVehicleType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "required_skills", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> requiredSkills = new ArrayList<>();

    // Distance and Cost
    @Column(name = "distance")
    private Double distance;

    @Column(name = "estimated_duration")
    private Integer estimatedDuration;

    @Column(name = "estimated_cost")
    private Double estimatedCost;

    // Assignment Information
    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "assigned_by")
    private String assignedBy;

    @Column(name = "optimization_batch_id")
    private String optimizationBatchId;

    // Flags
    @Column(name = "is_round_trip", columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    private Boolean isRoundTrip = false;

    @Column(name = "is_sequential", columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    private Boolean isSequential = false;

    // Audit
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Business Methods
    public boolean isShortAppointment() {
        return appointmentDuration != null && appointmentDuration <= 15;
    }

    public boolean requiresSameDriver() {
        return Boolean.TRUE.equals(isRoundTrip) || isShortAppointment();
    }

    public void setPickupTimeWindow(LocalDateTime pickupTime, int windowMinutes) {
        this.pickupTime = pickupTime;
        this.pickupWindowStart = pickupTime.minusMinutes(windowMinutes);
        this.pickupWindowEnd = pickupTime.plusMinutes(windowMinutes);
    }

    public void setDropoffTimeWindow(LocalDateTime dropoffTime, int windowMinutes) {
        this.dropoffTime = dropoffTime;
        this.dropoffWindowStart = dropoffTime.minusMinutes(windowMinutes);
        this.dropoffWindowEnd = dropoffTime.plusMinutes(windowMinutes);
    }

    public boolean isAssigned() {
        return pickupDriver != null || dropoffDriver != null;
    }

    public boolean isFullyAssigned() {
        if (Boolean.TRUE.equals(isRoundTrip)) {
            return pickupDriver != null;
        }
        return pickupDriver != null && dropoffDriver != null;
    }

    // Legacy methods for backward compatibility
    public Double getPickupLng() {
        return pickupLocation != null ? pickupLocation.getLongitude() : null;
    }

    public Double getPickupLat() {
        return pickupLocation != null ? pickupLocation.getLatitude() : null;
    }

    public Double getDropoffLng() {
        return dropoffLocation != null ? dropoffLocation.getLongitude() : null;
    }

    public Double getDropoffLat() {
        return dropoffLocation != null ? dropoffLocation.getLatitude() : null;
    }

    // Helper methods for location strings
    public String getPickupLocationString() {
        if (pickupLocationString != null) return pickupLocationString;
        return pickupLocation != null ? pickupLocation.getAddress() : null;
    }

    public String getDropoffLocationString() {
        if (dropoffLocationString != null) return dropoffLocationString;
        return dropoffLocation != null ? dropoffLocation.getAddress() : null;
    }
}