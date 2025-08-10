package com.mediroute.entity;

import com.mediroute.dto.RideStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Entity
@Table(name = "patient_history", indexes = {
        @Index(name = "idx_history_patient", columnList = "patient_id"),
        @Index(name = "idx_history_date", columnList = "ride_date"),
        @Index(name = "idx_history_status", columnList = "ride_status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@ToString(exclude = {"patient", "ride"}) // Prevent circular references
@EqualsAndHashCode(exclude = {"patient", "ride"})
@Schema(description = "Historical record of patient rides")
public class PatientHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique history record identifier")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    @Schema(description = "Patient this history belongs to")
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id")
    @Schema(description = "Associated ride")
    private Ride ride;

    // Ride Information
    @Column(name = "ride_date", nullable = false)
    @Schema(description = "Date of the ride")
    private LocalDateTime rideDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "ride_status")
    @Schema(description = "Final status of the ride")
    private RideStatus rideStatus;

    // Location Information
    @Column(name = "pickup_location")
    @Schema(description = "Pickup location")
    private String pickupLocation;

    @Column(name = "dropoff_location")
    @Schema(description = "Dropoff location")
    private String dropoffLocation;

    // Metrics
    @Column(name = "distance")
    @Schema(description = "Distance traveled in kilometers")
    private Double distance;

    @Column(name = "duration_minutes")
    @Schema(description = "Actual ride duration in minutes")
    private Integer durationMinutes;

    @Column(name = "cost")
    @Schema(description = "Ride cost")
    private Double cost;

    // Driver Information
    @Column(name = "driver_name")
    @Schema(description = "Name of the driver")
    private String driverName;

    @Column(name = "vehicle_type")
    @Schema(description = "Type of vehicle used")
    private String vehicleType;

    // Quality Metrics
    @Column(name = "on_time_pickup", columnDefinition = "BOOLEAN DEFAULT TRUE")
    @Builder.Default
    @Schema(description = "Whether pickup was on time")
    private Boolean onTimePickup = true;

    @Column(name = "on_time_dropoff", columnDefinition = "BOOLEAN DEFAULT TRUE")
    @Builder.Default
    @Schema(description = "Whether dropoff was on time")
    private Boolean onTimeDropoff = true;

    @Column(name = "patient_satisfaction")
    @Schema(description = "Patient satisfaction score (1-5)")
    private Integer patientSatisfaction;

    // History Notes - Use unique column name
    @Column(name = "history_notes", columnDefinition = "TEXT")
    @Schema(description = "Additional notes about the ride")
    private String historyNotes;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Business Methods
    public boolean wasSuccessful() {
        return rideStatus == RideStatus.COMPLETED;
    }

    public boolean wasOnTime() {
        return Boolean.TRUE.equals(onTimePickup) && Boolean.TRUE.equals(onTimeDropoff);
    }
}