package com.mediroute.entity;

import com.mediroute.dto.RideAssignmentAuditDTO;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "assignment_audit")
public class AssignmentAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "assignment_time")
    private LocalDateTime assignmentTime;

    @Column(name = "assignment_date")
    private LocalDate assignmentDate;

    @Column(name = "batch_id")
    private String batchId;

    // Ride Statistics
    @Column(name = "total_rides")
    private Integer totalRides;

    @Column(name = "assigned_rides")
    private Integer assignedRides;

    @Column(name = "unassigned_rides")
    private Integer unassignedRides;

    // Driver Statistics
    @Column(name = "assigned_drivers")
    private Integer assignedDrivers;

    @Column(name = "total_available_drivers")
    private Integer totalAvailableDrivers;

    // Medical Transport Statistics
    @Column(name = "wheelchair_rides")
    private Integer wheelchairRides = 0;

    @Column(name = "stretcher_rides")
    private Integer stretcherRides = 0;

    @Column(name = "round_trip_rides")
    private Integer roundTripRides = 0;

    @Column(name = "emergency_rides")
    private Integer emergencyRides = 0;

    // Performance Metrics
    @Column(name = "success_rate")
    private Double successRate;

    @Column(name = "average_assignment_time_seconds")
    private Integer averageAssignmentTimeSeconds;

    @Column(name = "optimization_strategy")
    private String optimizationStrategy;

    // Metadata
    @Column(name = "triggered_by")
    private String triggeredBy;

    // Assignment Data (JSON)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ride_assignments", columnDefinition = "jsonb")
    private Map<Long, List<Long>> rideAssignmentsSummary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ride_assignments_detail", columnDefinition = "jsonb")
    private List<RideAssignmentAuditDTO> rideAssignmentsDetail;

    // Constraints and Issues
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "unassigned_reasons", columnDefinition = "jsonb")
    private Map<Long, String> unassignedReasons;

    @Column(name = "constraint_violations")
    private Integer constraintViolations = 0;

    // Timestamps
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (assignmentTime == null) {
            assignmentTime = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper methods for medical transport
    public double calculateSuccessRate() {
        if (totalRides == null || totalRides == 0) {
            return 0.0;
        }
        int assigned = assignedRides != null ? assignedRides : 0;
        return (assigned * 100.0) / totalRides;
    }

    public void updateSuccessRate() {
        this.successRate = calculateSuccessRate();
    }

    public void addUnassignedRide(Long rideId, String reason) {
        if (this.unassignedReasons == null) {
            this.unassignedReasons = new HashMap<>();
        }
        this.unassignedReasons.put(rideId, reason);
    }

    public boolean hasUnassignedRides() {
        return unassignedRides != null && unassignedRides > 0;
    }

    public int getTotalMedicalTransportRides() {
        int wheelchair = wheelchairRides != null ? wheelchairRides : 0;
        int stretcher = stretcherRides != null ? stretcherRides : 0;
        return wheelchair + stretcher;
    }

    public String getOptimizationSummary() {
        return String.format("Batch %s: %d/%d rides assigned (%.1f%%) using %s",
                batchId,
                assignedRides != null ? assignedRides : 0,
                totalRides != null ? totalRides : 0,
                successRate != null ? successRate : 0.0,
                optimizationStrategy != null ? optimizationStrategy : "Unknown");
    }
}
