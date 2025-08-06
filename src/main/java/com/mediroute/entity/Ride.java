package com.mediroute.entity;

import com.mediroute.dto.Priority;
import com.mediroute.dto.RideStatus;
import com.mediroute.dto.RideType;
import com.mediroute.dto.VehicleTypeEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;


// Enhanced Ride Entity (extends your current one)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rides")
public class Ride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "patient_id")
    private Patient patient;

    // ENHANCED: Split driver assignment for medical transport
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pickup_driver_id")
    private Driver pickupDriver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dropoff_driver_id")
    private Driver dropoffDriver;

    // Keep backward compatibility
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Driver driver; // Your existing field - still works

    @Column(nullable = false)
    private String pickupLocation;

    @Column(nullable = false)
    private String dropoffLocation;

    @Column(name = "pickup_lat")
    private Double pickupLat;

    @Column(name = "pickup_lng")
    private Double pickupLng;

    @Column(name = "dropoff_lat")
    private Double dropoffLat;

    @Column(name = "dropoff_lng")
    private Double dropoffLng;

    @Column(nullable = false)
    private LocalDateTime pickupTime;

    // NEW: Medical transport specific fields
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

    @Enumerated(EnumType.STRING)
    @Column(name = "ride_type")
    private RideType rideType;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority")
    private Priority priority;

    @Column(name = "is_round_trip", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isRoundTrip;

    // Your existing fields
    @Column(name = "wait_time", columnDefinition = "INT DEFAULT 0 CHECK (wait_time BETWEEN 0 AND 15)")
    private Integer waitTime;

    @Column(name = "is_sequential", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isSequential;

    @Column(name = "distance")
    private Float distance;

    @Column(name = "route_distance")
    private Float routeDistance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private RideStatus status;

    @Column(name = "required_vehicle_type")
    private String requiredVehicleType;

    @Transient
    private LocalTime timeOnly;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> requiredSkills;

    @Column(name = "return_ride_id")
    private Long returnRideId;

    @Column(name = "estimated_cost")
    private Float estimatedCost;

    @Column(name = "estimated_duration")
    private Integer estimatedDuration;

    // NEW: Assignment tracking
    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "assigned_by")
    private String assignedBy;

    @Column(name = "optimization_batch_id")
    private String optimizationBatchId;

    // Helper methods for medical transport
    public boolean isShortAppointment() {
        return appointmentDuration != null && appointmentDuration <= 15;
    }

    public boolean requiresSameDriver() {
        return isRoundTrip != null && isRoundTrip;
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
}
