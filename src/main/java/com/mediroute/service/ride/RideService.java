package com.mediroute.service.ride;

import com.mediroute.dto.*;
import com.mediroute.entity.Ride;
import com.mediroute.exceptions.RideNotFoundException;
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

    // ========== SAFE LAZY LOADING METHODS ==========

    public Optional<Ride> findById(Long id) {
        return rideRepository.findByIdWithFullDetails(id);
    }

    public Optional<Ride> findByIdWithAudit(Long id) {
        return rideRepository.findByIdWithAudit(id);
    }

    public List<Ride> findRidesByDate(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        return rideRepository.findByPickupTimeBetweenWithDriversAndPatient(start, end);
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
    public List<RideDetailDTO> findRidesByDateAsDTO(LocalDate date) {
        return findRidesByDate(date).stream()
                .map(RideDetailDTO::fromEntity)
                .collect(Collectors.toList());
    }

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
}
