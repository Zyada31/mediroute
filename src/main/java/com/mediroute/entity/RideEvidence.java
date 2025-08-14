package com.mediroute.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "ride_evidence", indexes = {
        @Index(name = "idx_evidence_ride", columnList = "ride_id"),
        @Index(name = "idx_evidence_type", columnList = "type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RideEvidence {

    public enum EvidenceType { PICKUP, DROPOFF, INCIDENT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ride_id", nullable = false)
    private Ride ride;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private EvidenceType type;

    @Column(name = "file_path", nullable = false, length = 1024)
    private String filePath;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "lat")
    private Double lat;

    @Column(name = "lng")
    private Double lng;

    @Column(name = "notes", length = 4000)
    private String notes;

    @Column(name = "event_time")
    private Instant eventTime;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (eventTime == null) eventTime = Instant.now();
    }
}


