package com.mediroute.service.ride;

import com.mediroute.dto.*;
import com.mediroute.entity.Ride;
import com.mediroute.repository.RideRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import static com.mediroute.config.SecurityBeans.currentOrgId;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RideService {

    private final RideRepository rideRepository;
    private final RideAuditService rideAuditService;

    /**
     * Find ride by ID with all associations loaded
     */
    @Transactional(readOnly = true)
    public Optional<Ride> findById(Long id) {
        return rideRepository.findByIdWithFullDetails(id);
    }

    /**
     * Find rides by date with all associations properly loaded
     */
    @Transactional(readOnly = true)
    public List<RideDetailDTO> findRidesByDate(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        // Use JOIN FETCH query to avoid lazy loading issues
        List<Ride> rides = rideRepository.findByPickupTimeBetweenWithDriversAndPatient(start, end);

        // Convert to DTOs within transaction to avoid lazy loading
        return rides.stream()
                .map(this::safeConvertToDTO)
                .collect(Collectors.toList());
    }

    // Pageable variants for API listing
    @Transactional(readOnly = true)
    public Page<Ride> findRidesByDatePaged(LocalDate date, Pageable pageable) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        Long org = currentOrgId();
        if (org != null) {
            return rideRepository.findByOrgIdAndPickupTimeBetween(org, start, end, pageable);
        }
        return rideRepository.findByPickupTimeBetween(start, end, pageable);
    }

    /**
     * Find rides by date and status with proper initialization
     */
    @Transactional(readOnly = true)
    public List<Ride> findRidesByDateAndStatus(LocalDate date, RideStatus status) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        List<Ride> rides = rideRepository.findByPickupTimeBetweenAndStatusWithDriversAndPatient(start, end, status);

        // Initialize entities within transaction
        rides.forEach(this::initializeRideEntities);

        return rides;
    }

    /**
     * Find unassigned rides with proper entity initialization
     */
    @Transactional(readOnly = true)
    public List<Ride> findUnassignedRides(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        List<Ride> rides = rideRepository.findUnassignedRidesInTimeRangeWithPatient(start, end);

        // Initialize entities within transaction
        rides.forEach(this::initializeRideEntities);

        return rides;
    }

    @Transactional(readOnly = true)
    public Page<Ride> findUnassignedRidesPaged(LocalDate date, Pageable pageable) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        Long org = currentOrgId();
        if (org != null) {
            return rideRepository.findByOrgIdAndStatusAndPickupDriverIsNullAndDropoffDriverIsNullAndDriverIsNullAndPickupTimeBetween(
                    org, RideStatus.SCHEDULED, start, end, pageable);
        }
        return rideRepository.findByStatusAndPickupDriverIsNullAndDropoffDriverIsNullAndDriverIsNullAndPickupTimeBetween(
                RideStatus.SCHEDULED, start, end, pageable);
    }

    /**
     * Find rides by driver with proper initialization
     */
    @Transactional(readOnly = true)
    public List<Ride> findRidesByDriver(Long driverId, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        List<Ride> rides = rideRepository.findByAnyDriverAndPickupTimeBetweenWithPatient(driverId, start, end);

        // Initialize entities within transaction
        rides.forEach(this::initializeRideEntities);

        return rides;
    }

    /**
     * Get unassigned rides as DTOs to prevent lazy loading issues
     */
    @Transactional(readOnly = true)
    public List<RideDetailDTO> findUnassignedRidesAsDTO(LocalDate date) {
        return findUnassignedRides(date).stream()
                .map(this::safeConvertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Save ride with proper transaction management
     */
    @Transactional
    public Ride save(Ride ride) {
        log.debug("Saving ride for patient: {}",
                ride.getPatient() != null ? ride.getPatient().getName() : "Unknown");
        return rideRepository.save(ride);
    }

    @Transactional
    public Ride updateStatus(Long rideId, RideStatus newStatus, Long actingDriverId, String actorRole, String notes) {
        Ride ride = rideRepository.findByIdWithFullDetails(rideId).orElseThrow();

        // Authorization: drivers can only update their assigned rides
        boolean isDriver = actorRole != null && actorRole.contains("DRIVER");
        if (isDriver) {
            Long pickupId = ride.getPickupDriver() != null ? ride.getPickupDriver().getId() : null;
            Long dropId   = ride.getDropoffDriver() != null ? ride.getDropoffDriver().getId() : null;
            Long legacyId = ride.getDriver() != null ? ride.getDriver().getId() : null;
            if (actingDriverId == null || !(actingDriverId.equals(pickupId) || actingDriverId.equals(dropId) || actingDriverId.equals(legacyId))) {
                throw new IllegalArgumentException("Driver not assigned to this ride");
            }
        }

        RideStatus old = ride.getStatus();
        boolean allowed = isAllowedTransition(old, newStatus);

        boolean isAdminOrDispatcher = actorRole != null && (actorRole.contains("ADMIN") || actorRole.contains("DISPATCHER"));
        boolean adminFastPath = false;
        if (!allowed && isAdminOrDispatcher && newStatus == RideStatus.COMPLETED && isFastCompleteEligible(old)) {
            // Admin/dispatcher fast path to complete
            allowed = true;
            adminFastPath = true;
        }

        if (!allowed) {
            throw new IllegalArgumentException("Invalid status transition: " + old + " -> " + newStatus);
        }

        ride.setStatus(newStatus);
        Ride saved = rideRepository.save(ride);
        rideAuditService.auditChange(saved, "status", old, newStatus, actorRole != null ? actorRole : "SYSTEM");
        if (adminFastPath) {
            rideAuditService.auditChange(saved, "admin_fast_complete", old, newStatus, actorRole != null ? actorRole : "SYSTEM");
        }
        if (notes != null && !notes.isBlank()) {
            rideAuditService.auditChange(saved, "notes", "", notes, actorRole != null ? actorRole : "SYSTEM");
        }
        return saved;
    }

    private boolean isAllowedTransition(RideStatus from, RideStatus to) {
        if (from == null) return to == RideStatus.SCHEDULED || to == RideStatus.ASSIGNED; // initialization
        switch (from) {
            case SCHEDULED:
                return to == RideStatus.ASSIGNED || to == RideStatus.CANCELLED;
            case ASSIGNED:
                return to == RideStatus.EN_ROUTE_PICKUP || to == RideStatus.CANCELLED || to == RideStatus.NO_SHOW;
            case EN_ROUTE_PICKUP:
                return to == RideStatus.ARRIVED_PICKUP || to == RideStatus.CANCELLED || to == RideStatus.NO_SHOW;
            case ARRIVED_PICKUP:
                return to == RideStatus.EN_ROUTE_DROPOFF || to == RideStatus.CANCELLED;
            case EN_ROUTE_DROPOFF:
                return to == RideStatus.ARRIVED_DROPOFF || to == RideStatus.CANCELLED;
            case ARRIVED_DROPOFF:
                return to == RideStatus.COMPLETED || to == RideStatus.CANCELLED;
            case COMPLETED:
            case CANCELLED:
            case NO_SHOW:
                return false;
            default:
                return false;
        }
    }

    private boolean isFastCompleteEligible(RideStatus from) {
        switch (from) {
            case ASSIGNED:
            case EN_ROUTE_PICKUP:
            case ARRIVED_PICKUP:
            case EN_ROUTE_DROPOFF:
            case ARRIVED_DROPOFF:
                return true;
            default:
                return false;
        }
    }

    @Transactional
    public void deleteById(Long id) {
        log.info("Deleting ride with ID: {}", id);
        rideRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return rideRepository.existsById(id);
    }

    @Transactional(readOnly = true)
    public long count() {
        return rideRepository.count();
    }

    /**
     * Get ride statistics with proper entity initialization
     */
    @Transactional(readOnly = true)
    public RideStatisticsDTO getRideStatistics(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        List<Ride> allRides = rideRepository.findByPickupTimeBetweenWithDriversAndPatient(start, end);

        // Process within transaction to avoid lazy loading
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

    /**
     * CRITICAL: Initialize all lazy-loaded entities within transaction
     */
    private void initializeRideEntities(Ride ride) {
        try {
            // Initialize patient
            if (ride.getPatient() != null) {
                Hibernate.initialize(ride.getPatient());
                // Access key properties to ensure full initialization
                ride.getPatient().getName();
                ride.getPatient().getRequiresWheelchair();
                ride.getPatient().getRequiresStretcher();
                ride.getPatient().getRequiresOxygen();
                ride.getPatient().getMobilityLevel();

                // Initialize collections
                if (ride.getPatient().getMedicalConditions() != null) {
                    Hibernate.initialize(ride.getPatient().getMedicalConditions());
                }
                if (ride.getPatient().getSpecialNeeds() != null) {
                    Hibernate.initialize(ride.getPatient().getSpecialNeeds());
                }
            }

            // Initialize drivers
            if (ride.getPickupDriver() != null) {
                Hibernate.initialize(ride.getPickupDriver());
                ride.getPickupDriver().getName(); // Force access
                ride.getPickupDriver().getVehicleType();
                ride.getPickupDriver().getWheelchairAccessible();
                ride.getPickupDriver().getStretcherCapable();
                ride.getPickupDriver().getOxygenEquipped();
            }

            if (ride.getDropoffDriver() != null) {
                Hibernate.initialize(ride.getDropoffDriver());
                ride.getDropoffDriver().getName(); // Force access
            }

            if (ride.getDriver() != null) {
                Hibernate.initialize(ride.getDriver());
                ride.getDriver().getName(); // Force access
            }

            // Initialize location objects
            if (ride.getPickupLocation() != null) {
                ride.getPickupLocation().getAddress();
                ride.getPickupLocation().getLatitude();
                ride.getPickupLocation().getLongitude();
            }

            if (ride.getDropoffLocation() != null) {
                ride.getDropoffLocation().getAddress();
                ride.getDropoffLocation().getLatitude();
                ride.getDropoffLocation().getLongitude();
            }

        } catch (Exception e) {
            log.warn("Failed to initialize ride {} entities: {}", ride.getId(), e.getMessage());
        }
    }

    /**
     * Safely convert ride to DTO within transaction
     */
    private RideDetailDTO safeConvertToDTO(Ride ride) {
        try {
            // Ensure entities are initialized
            initializeRideEntities(ride);

            // Convert to DTO
            return RideDetailDTO.fromEntity(ride);

        } catch (Exception e) {
            log.error("Error converting ride {} to DTO: {}", ride.getId(), e.getMessage(), e);

            // Return minimal DTO with available data
            return RideDetailDTO.builder()
                    .id(ride.getId())
                    .pickupTime(ride.getPickupTime())
                    .dropoffTime(ride.getDropoffTime())
                    .status(ride.getStatus())
                    .priority(ride.getPriority())
                    .requiredVehicleType(ride.getRequiredVehicleType())
                    .distance(ride.getDistance())
                    .estimatedDuration(ride.getEstimatedDuration())
                    .isRoundTrip(ride.getIsRoundTrip())
                    .build();
        }
    }

    /**
     * Prepare rides for optimization with proper entity initialization
     */
    @Transactional(readOnly = true)
    public List<Ride> prepareRidesForOptimization(List<Long> rideIds) {
        if (rideIds == null || rideIds.isEmpty()) {
            return List.of();
        }

        // Load rides with all associations
        List<Ride> rides = rideRepository.findByIdInWithPatient(rideIds);

        // Initialize all entities within transaction
        rides.forEach(this::initializeRideEntities);

        log.info("Prepared {} rides for optimization with all entities initialized", rides.size());
        return rides;
    }

    /**
     * Get rides for optimization in date range
     */
    @Transactional(readOnly = true)
    public List<Ride> findRidesForOptimization(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        // Get rides with valid coordinates for optimization
        List<Ride> rides = rideRepository.findRidesWithValidCoordinatesAndPatient(start, end);

        // Initialize all entities within transaction
        rides.forEach(this::initializeRideEntities);

        log.info("Found {} rides with valid coordinates for optimization on {}", rides.size(), date);
        return rides;
    }
}