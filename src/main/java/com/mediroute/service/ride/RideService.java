package com.mediroute.service.ride;

import com.mediroute.dto.*;
import com.mediroute.entity.Ride;
import com.mediroute.repository.RideRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)  // Default to read-only for all methods
public class RideService {

    private final RideRepository rideRepository;
    public Optional<Ride> findById(Long id) {
        return rideRepository.findByIdWithFullDetails(id);
    }

    public Optional<Ride> findByIdWithAudit(Long id) {
        return rideRepository.findByIdWithAudit(id);
    }

    @Transactional(readOnly = true)
    public List<RideDetailDTO> findRidesByDate(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        // Use the JOIN FETCH query
        return rideRepository.findByPickupTimeBetweenWithDriversAndPatient(start, end)
                .stream()
                .map(RideDetailDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<Ride> findRidesByDateAndStatus(LocalDate date, RideStatus status) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        return rideRepository.findByPickupTimeBetweenAndStatusWithDriversAndPatient(start, end, status);
    }

    public List<Ride> findUnassignedRides(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        return rideRepository.findUnassignedRidesInTimeRangeWithPatient(start, end);
    }

    public List<Ride> findRidesByDriver(Long driverId, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        return rideRepository.findByAnyDriverAndPickupTimeBetweenWithPatient(driverId, start, end);
    }

    // Convert to DTOs to prevent lazy loading issues in controllers
//    public List<RideDetailDTO> findRidesByDateAsDTO(LocalDate date) {
//        return findRidesByDate(date).stream()
//                .map(RideDetailDTO::fromEntity)
//                .collect(Collectors.toList());
//    }

    public List<RideDetailDTO> findUnassignedRidesAsDTO(LocalDate date) {
        return findUnassignedRides(date).stream()
                .map(RideDetailDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional  // Write operation needs full transaction
    public Ride save(Ride ride) {
        log.debug("Saving ride for patient: {}",
                ride.getPatient() != null ? ride.getPatient().getName() : "Unknown");
        return rideRepository.save(ride);
    }

    @Transactional
    public void deleteById(Long id) {
        log.info("Deleting ride with ID: {}", id);
        rideRepository.deleteById(id);
    }

    public boolean existsById(Long id) {
        return rideRepository.existsById(id);
    }

    public long count() {
        return rideRepository.count();
    }

    // Statistics method that doesn't trigger lazy loading
    public RideStatisticsDTO getRideStatistics(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        List<Ride> allRides = rideRepository.findByPickupTimeBetweenWithDriversAndPatient(start, end);

        long totalRides = allRides.size();
        long assignedRides = allRides.stream()
                .filter(Ride::isAssigned)
                .count();
        long emergencyRides = allRides.stream()
                .filter(ride -> ride.getPriority() == Priority.EMERGENCY)
                .count();
        long wheelchairRides = allRides.stream()
                .filter(ride -> "wheelchair_van".equals(ride.getRequiredVehicleType()))
                .count();
        long roundTripRides = allRides.stream()
                .filter(ride -> Boolean.TRUE.equals(ride.getIsRoundTrip()))
                .count();

        return RideStatisticsDTO.builder()
                .date(date)
                .totalRides((int) totalRides)
                .assignedRides((int) assignedRides)
                .unassignedRides((int) (totalRides - assignedRides))
                .emergencyRides((int) emergencyRides)
                .wheelchairRides((int) wheelchairRides)
                .roundTripRides((int) roundTripRides)
                .assignmentRate(totalRides > 0 ? (assignedRides * 100.0 / totalRides) : 0.0)
                .build();
    }
//    private final StoragePort storage; // S3/local adapter
//    private final RideEvidenceRepo repo;
//    private final AuthContext auth; // to get driverId
//
//    public RideEvidenceDTO create(Long rideId, RideEvidenceEventType type, String note,
//                                  Double lat, Double lng,
//                                  MultipartFile signature, List<MultipartFile> photos) {
//        if ((type == RideEvidenceEventType.NO_SHOW || type == RideEvidenceEventType.CANCELLED)
//                && (note == null || note.isBlank())) {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "note required for NO_SHOW/CANCELLED");
//        }
//
//        String sigUrl = signature != null && !signature.isEmpty()
//                ? storage.put(signature, "evidence/signature/") : null;
//
//        List<String> photoUrls = Optional.ofNullable(photos).orElse(List.of()).stream()
//                .filter(p -> p != null && !p.isEmpty())
//                .limit(3)
//                .map(p -> storage.put(p, "evidence/photo/"))
//                .toList();
//
//        var entity = repo.save(RideEvidenceEntity.from(rideId, type, note, lat, lng, sigUrl, photoUrls, auth.driverId()));
//        // (optional) auto-advance status on DROPOFF or set NO_SHOW/CANCELLED here.
//
//        return entity.toDto();
//    }
//
//    public List<RideEvidenceDTO> listByRide(Long rideId) { return repo.findAllByRideId(rideId).stream().map(RideEvidenceEntity::toDto).toList(); }
}


