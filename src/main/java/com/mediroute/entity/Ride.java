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
import io.swagger.v3.oas.annotations.media.Schema;

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
@Schema(description = "Medical transport ride")
public class Ride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique ride identifier")
    private Long id;

    // Patient and Driver Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    @Schema(description = "Patient for this ride")
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pickup_driver_id")
    @Schema(description = "Driver for pickup")
    private Driver pickupDriver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dropoff_driver_id")
    @Schema(description = "Driver for dropoff")
    private Driver dropoffDriver;

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

    // Timing Information
    @Column(name = "pickup_time", nullable = false)
    @Schema(description = "Scheduled pickup time")
    private LocalDateTime pickupTime;

    @Column(name = "dropoff_time")
    @Schema(description = "Scheduled dropoff time")
    private LocalDateTime dropoffTime;

    @Column(name = "pickup_window_start")
    @Schema(description = "Earliest acceptable pickup time")
    private LocalDateTime pickupWindowStart;

    @Column(name = "pickup_window_end")
    @Schema(description = "Latest acceptable pickup time")
    private LocalDateTime pickupWindowEnd;

    @Column(name = "dropoff_window_start")
    @Schema(description = "Earliest acceptable dropoff time")
    private LocalDateTime dropoffWindowStart;

    @Column(name = "dropoff_window_end")
    @Schema(description = "Latest acceptable dropoff time")
    private LocalDateTime dropoffWindowEnd;

    @Column(name = "appointment_duration")
    @Schema(description = "Duration of appointment in minutes")
    private Integer appointmentDuration;

    // Ride Classification
    @Enumerated(EnumType.STRING)
    @Column(name = "ride_type")
    @Schema(description = "Type of ride (ONE_WAY, ROUND_TRIP)")
    private RideType rideType;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority")
    @Builder.Default
    @Schema(description = "Priority level")
    private Priority priority = Priority.ROUTINE;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    @Schema(description = "Current ride status")
    private RideStatus status = RideStatus.SCHEDULED;

    // Vehicle and Requirements
    @Column(name = "required_vehicle_type")
    @Schema(description = "Required vehicle type")
    private String requiredVehicleType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "required_skills", columnDefinition = "jsonb")
    @Builder.Default
    @Schema(description = "Required driver skills")
    private List<String> requiredSkills = new ArrayList<>();

    // Distance and Cost
    @Column(name = "distance")
    @Schema(description = "Distance in kilometers")
    private Double distance;

    @Column(name = "estimated_duration")
    @Schema(description = "Estimated duration in minutes")
    private Integer estimatedDuration;

    @Column(name = "estimated_cost")
    @Schema(description = "Estimated cost")
    private Double estimatedCost;

    // Assignment Information
    @Column(name = "assigned_at")
    @Schema(description = "When ride was assigned")
    private LocalDateTime assignedAt;

    @Column(name = "assigned_by")
    @Schema(description = "Who assigned the ride")
    private String assignedBy;

    @Column(name = "optimization_batch_id")
    @Schema(description = "Optimization batch identifier")
    private String optimizationBatchId;

    // Flags
    @Column(name = "is_round_trip", columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    @Schema(description = "Whether this is a round trip")
    private Boolean isRoundTrip = false;

    @Column(name = "is_sequential", columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    @Schema(description = "Whether pickup and dropoff must be sequential")
    private Boolean isSequential = false;

    // Audit
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationships
    @OneToMany(mappedBy = "ride", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RideAudit> auditHistory = new ArrayList<>();

    @OneToOne(mappedBy = "ride", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private RideConstraint constraints;

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

    public double getPickupLng() {
    }
}