package com.mediroute.controller;

import com.mediroute.entity.Ride;
import com.mediroute.entity.RideEvidence;
import com.mediroute.repository.RideEvidenceRepository;
import com.mediroute.repository.RideRepository;
import com.mediroute.service.storage.StorageService;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Manage ride evidence uploads and listing.
 * Uses StorageService for persistence; returns signed URLs for access.
 */
@RestController
@RequestMapping("/api/v1/rides/{rideId}/evidence")
@Tag(name = "Ride Evidence", description = "Upload and list ride proof-of-service evidence")
public class RideEvidenceController {

    private final RideRepository rideRepository;
    private final RideEvidenceRepository evidenceRepository;
    private final StorageService storage;

    public RideEvidenceController(RideRepository rideRepository, RideEvidenceRepository evidenceRepository, StorageService storage) {
        this.rideRepository = rideRepository;
        this.evidenceRepository = evidenceRepository;
        this.storage = storage;
    }

    @PostMapping(consumes = {"multipart/form-data"})
    @Operation(summary = "Upload ride evidence")
    @PreAuthorize("hasAnyRole('DRIVER','DISPATCHER','ADMIN','PROVIDER')")
    public ResponseEntity<?> upload(
            @PathVariable Long rideId,
            @RequestParam("type") @Parameter(description = "Evidence type, e.g., PICKUP/DROPOFF") RideEvidence.EvidenceType type,
            @RequestParam(value = "lat", required = false) Double lat,
            @RequestParam(value = "lng", required = false) Double lng,
            @RequestParam(value = "notes", required = false) String notes,
            @RequestParam(value = "eventTime", required = false) Long eventTimeEpochSec,
            @RequestParam("file") MultipartFile file
    ) throws Exception {
        Ride ride = rideRepository.findById(rideId).orElseThrow();

        var stored = storage.store(file.getInputStream(), file.getContentType(), file.getOriginalFilename(), Map.of());

        RideEvidence ev = RideEvidence.builder()
                .ride(ride)
                .type(type)
                .filePath(stored.objectId())
                .contentType(stored.contentType())
                .fileSizeBytes(stored.sizeBytes())
                .lat(lat)
                .lng(lng)
                .notes(notes)
                .eventTime(eventTimeEpochSec != null ? Instant.ofEpochSecond(eventTimeEpochSec) : Instant.now())
                .build();
        evidenceRepository.save(ev);
        return ResponseEntity.ok(Map.of(
                "id", ev.getId(),
                "url", storage.getSignedReadUrl(stored.objectId(), java.time.Duration.ofMinutes(5))
        ));
    }

    @GetMapping
    @Operation(summary = "List ride evidence")
    @PreAuthorize("hasAnyRole('DRIVER','DISPATCHER','ADMIN','PROVIDER')")
    public List<Map<String,Object>> list(@PathVariable Long rideId) {
        return evidenceRepository.findAllByRideIdOrderByCreatedAtAsc(rideId).stream().map(v -> {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("id", v.getId());
            m.put("type", v.getType());
            m.put("createdAt", v.getCreatedAt());
            m.put("eventTime", v.getEventTime());
            m.put("lat", v.getLat());
            m.put("lng", v.getLng());
            m.put("notes", v.getNotes());
            m.put("contentType", v.getContentType());
            m.put("sizeBytes", v.getFileSizeBytes());
            m.put("url", storage.getSignedReadUrl(v.getFilePath(), java.time.Duration.ofMinutes(5)));
            return m;
        }).toList();
    }
}


