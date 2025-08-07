package com.mediroute.entity;

import com.mediroute.dto.RideStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "schedules",
        uniqueConstraints = @UniqueConstraint(columnNames = {"ride_id", "date"}),
        indexes = {
                @Index(name = "idx_schedule_date", columnList = "date"),
                @Index(name = "idx_schedule_driver", columnList = "assigned_driver_id"),
                @Index(name = "idx_schedule_status", columnList = "status")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@Schema(description = "Daily schedule entry for rides")
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique schedule identifier")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id", nullable = false)
    @Schema(description = "Scheduled ride")
    private Ride ride;

    @Column(name = "date", nullable = false)
    @Schema(description = "Schedule date")
    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_driver_id")
    @Schema(description = "Assigned driver")
    private Driver assignedDriver;

    // Schedule Information
    @Column(name = "sequence_number")
    @Schema(description = "Order in driver's daily sequence")
    private Integer sequenceNumber;

    @Column(name = "estimated_start_time")
    @Schema(description = "Estimated start time for this ride")
    private LocalDateTime estimatedStartTime;

    @Column(name = "estimated_end_time")
    @Schema(description = "Estimated completion time")
    private LocalDateTime estimatedEndTime;

    @Column(name = "actual_start_time")
    @Schema(description = "Actual start time")
    private LocalDateTime actualStartTime;

    @Column(name = "actual_end_time")
    @Schema(description = "Actual completion time")
    private LocalDateTime actualEndTime;

    // Status
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    @Schema(description = "Schedule status")
    private RideStatus status = RideStatus.SCHEDULED;

    // Optimization Information
    @Column(name = "optimization_batch_id")
    @Schema(description = "Batch ID from optimization")
    private String optimizationBatchId;

    @Column(name = "optimization_score")
    @Schema(description = "Optimization score for this assignment")
    private Double optimizationScore;

    // Notes
    @Column(name = "notes", columnDefinition = "TEXT")
    @Schema(description = "Schedule notes")
    private String notes;

    @Column(name = "created_by")
    @Schema(description = "Who created this schedule entry")
    private String createdBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Business Methods
    public boolean isCompleted() {
        return status == RideStatus.COMPLETED && actualEndTime != null;
    }

    public boolean isRunningLate() {
        if (estimatedStartTime == null || actualStartTime == null) {
            return false;
        }
        return actualStartTime.isAfter(estimatedStartTime.plusMinutes(10));
    }

    public Integer getDelayMinutes() {
        if (estimatedStartTime == null || actualStartTime == null) {
            return null;
        }
        return (int) java.time.Duration.between(estimatedStartTime, actualStartTime).toMinutes();
    }
}