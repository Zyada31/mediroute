package com.mediroute.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Entity
@Table(name = "ride_constraints", indexes = {
        @Index(name = "idx_constraint_ride", columnList = "ride_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@Schema(description = "Additional constraints for ride scheduling")
public class RideConstraint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique constraint identifier")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id", nullable = false)
    @Schema(description = "Associated ride")
    private Ride ride;

    // Time Constraints
    @Column(name = "earliest_pickup")
    @Schema(description = "Earliest allowable pickup time")
    private LocalDateTime earliestPickup;

    @Column(name = "latest_dropoff")
    @Schema(description = "Latest allowable dropoff time")
    private LocalDateTime latestDropoff;

    @Column(name = "fixed_pickup_time", columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    @Schema(description = "Whether pickup time is fixed")
    private Boolean fixedPickupTime = false;

    // Driver Constraints
    @Column(name = "must_be_solo", columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    @Schema(description = "Patient must ride alone")
    private Boolean mustBeSolo = false;

    @Column(name = "preferred_driver_id")
    @Schema(description = "Preferred driver ID")
    private Long preferredDriverId;

    @Column(name = "excluded_driver_id")
    @Schema(description = "Driver to exclude")
    private Long excludedDriverId;

    // Route Constraints
    @Column(name = "max_detour_minutes")
    @Schema(description = "Maximum detour time in minutes")
    private Integer maxDetourMinutes;

    @Column(name = "direct_route_only", columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    @Schema(description = "Must use direct route")
    private Boolean directRouteOnly = false;

    // Special Requirements
    @Column(name = "requires_attendant", columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    @Schema(description = "Requires medical attendant")
    private Boolean requiresAttendant = false;

    @Column(name = "temperature_controlled", columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    @Schema(description = "Requires temperature controlled vehicle")
    private Boolean temperatureControlled = false;

    // Notes and Metadata
    @Column(name = "constraint_notes", columnDefinition = "TEXT")
    @Schema(description = "Additional constraint notes")
    private String constraintNotes;

    @Column(name = "created_by")
    @Schema(description = "Who created the constraint")
    private String createdBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
