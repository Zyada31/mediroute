package com.mediroute.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "optimization_jobs", indexes = {
        @Index(name = "idx_opt_jobs_status", columnList = "status"),
        @Index(name = "idx_opt_jobs_submitted", columnList = "submitted_at")
})
public class OptimizationJob {

    public enum JobType { DATE, RIDES }
    public enum JobStatus { PENDING, RUNNING, COMPLETED, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    private JobType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private JobStatus status = JobStatus.PENDING;

    @Column(name = "opt_date")
    private LocalDate date;

    @Column(name = "ride_ids", columnDefinition = "text")
    private String rideIdsCsv;

    @Column(name = "batch_id", length = 128)
    private String batchId;

    @Column(name = "error", columnDefinition = "text")
    private String error;

    @Column(name = "callback_url", length = 1024)
    private String callbackUrl;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // Getters and setters
    public Long getId() { return id; }
    public JobType getType() { return type; }
    public void setType(JobType type) { this.type = type; }
    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getRideIdsCsv() { return rideIdsCsv; }
    public void setRideIdsCsv(String rideIdsCsv) { this.rideIdsCsv = rideIdsCsv; }
    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public String getCallbackUrl() { return callbackUrl; }
    public void setCallbackUrl(String callbackUrl) { this.callbackUrl = callbackUrl; }
}


