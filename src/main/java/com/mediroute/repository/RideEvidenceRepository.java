package com.mediroute.repository;

import com.mediroute.entity.RideEvidence;
import com.mediroute.repository.base.BaseRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface RideEvidenceRepository extends BaseRepository<RideEvidence, Long> {
    // Entity fetch (avoid returning in controllers)
    List<RideEvidence> findByRideIdOrderByCreatedAtAsc(Long rideId);

    // Projection for safe serialization
    interface RideEvidenceView {
        Long getId();
        RideEvidence.EvidenceType getType();
        Instant getCreatedAt();
        Instant getEventTime();
        Double getLat();
        Double getLng();
        String getNotes();
        String getContentType();
        Long getFileSizeBytes();
        String getFilePath();
    }

    List<RideEvidenceView> findAllByRideIdOrderByCreatedAtAsc(Long rideId);
}


