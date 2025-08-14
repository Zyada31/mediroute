package com.mediroute.service.ride;

import com.mediroute.dto.*;
import com.mediroute.config.AppProps;
import com.mediroute.entity.AssignmentAudit;
import com.mediroute.entity.Driver;
import com.mediroute.entity.Patient;
import com.mediroute.entity.Ride;
import com.mediroute.repository.AssignmentAuditRepository;
import com.mediroute.repository.DriverRepository;
import com.mediroute.repository.RideRepository;
import com.mediroute.service.distance.OsrmDistanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.mediroute.config.SecurityBeans.currentOrgId;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnhancedMedicalTransportOptimizer {

    private final RideRepository rideRepository;
    private final DriverRepository driverRepository;
    private final OsrmDistanceService distanceService; // reserved for future distance-based scoring enhancements
    private final AssignmentAuditRepository assignmentAuditRepository;
    private final AppProps appProps;

    // Medical transport constants
    private static final int SHORT_APPOINTMENT_THRESHOLD = 15;
    // private static final int OPTIMIZATION_TIMEOUT_SECONDS = 45; // reserved
    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double MAX_PICKUP_DISTANCE_KM = 50.0;
    private static final double PREFERRED_PICKUP_DISTANCE_KM = 15.0;

    /**
     * Main optimization entry point
     */
    @Transactional
    public OptimizationResult optimizeSchedule(List<Ride> rides) {
        if (rides == null || rides.isEmpty()) {
            log.warn("⚠️ No rides to optimize");
            return OptimizationResult.empty();
        }

        // CRITICAL: Ensure all entities are initialized within transaction
        List<Ride> fullyLoadedRides = initializeRideEntities(rides);
        List<Driver> availableDrivers = getQualifiedDrivers();

        if (availableDrivers.isEmpty()) {
            log.warn("⚠️ No qualified drivers available for medical transport");
            return createUnassignedResult(fullyLoadedRides, "No qualified drivers available");
        }

        String batchId = generateBatchId();
        boolean osrmOk = distanceService.isOsrmHealthy();
        log.info("🏥 Starting enhanced medical transport optimization for {} rides with {} drivers (Batch: {}, OSRM healthy: {})",
                fullyLoadedRides.size(), availableDrivers.size(), batchId, osrmOk);

        try {
            return performMedicalTransportOptimization(fullyLoadedRides, availableDrivers, batchId);
        } catch (Exception e) {
            log.error("❌ Enhanced optimization failed: {}", e.getMessage(), e);
            return performIntelligentFallback(fullyLoadedRides, availableDrivers, batchId);
        }
    }

    /**
     * CRITICAL: Initialize all lazy-loaded entities within transaction
     */
    @Transactional(readOnly = true)
    public List<Ride> initializeRideEntities(List<Ride> rides) {
        List<Long> rideIds = rides.stream()
                .map(Ride::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (rideIds.isEmpty()) {
            // For new rides that don't have IDs yet, return as-is but initialize patients
            return rides.stream()
                    .peek(this::initializeRidePatient)
                    .collect(Collectors.toList());
        }

        // Load rides with all associations using JOIN FETCH
        List<Ride> fullyLoadedRides = rideRepository.findByIdInWithPatient(rideIds);

        // Force initialization of all lazy properties
        fullyLoadedRides.forEach(this::initializeRidePatient);

        return fullyLoadedRides;
    }

    /**
     * Initialize patient and driver entities to prevent lazy loading issues
     */
    private void initializeRidePatient(Ride ride) {
        try {
            if (ride.getPatient() != null) {
                // Force initialization of patient properties
                Hibernate.initialize(ride.getPatient());
                Patient patient = ride.getPatient();

                // Access key properties to fully initialize
                patient.getName();
                patient.getRequiresWheelchair();
                patient.getRequiresStretcher();
                patient.getRequiresOxygen();
                patient.getMobilityLevel();

                // Initialize collections if they exist
                if (patient.getMedicalConditions() != null) {
                    Hibernate.initialize(patient.getMedicalConditions());
                }
                if (patient.getSpecialNeeds() != null) {
                    Hibernate.initialize(patient.getSpecialNeeds());
                }
            }

            // Initialize drivers if already assigned
            if (ride.getPickupDriver() != null) {
                Hibernate.initialize(ride.getPickupDriver());
                ride.getPickupDriver().getName(); // Force access
            }
            if (ride.getDropoffDriver() != null) {
                Hibernate.initialize(ride.getDropoffDriver());
                ride.getDropoffDriver().getName(); // Force access
            }
            if (ride.getDriver() != null) {
                Hibernate.initialize(ride.getDriver());
                ride.getDriver().getName(); // Force access
            }

        } catch (Exception e) {
            log.warn("Failed to initialize ride {} entities: {}", ride.getId(), e.getMessage());
        }
    }

    /**
     * Core medical transport optimization using intelligent fallback algorithm
     */
    @Transactional
    public OptimizationResult performMedicalTransportOptimization(List<Ride> rides, List<Driver> drivers, String batchId) {
        RideCategorization categorization = categorizeRides(rides);
        OptimizationResult totalResult = OptimizationResult.create(batchId, rides.size());

        // Phase 1: Handle emergency rides first
        totalResult.merge(optimizeEmergencyRides(categorization.getEmergencyRides(), drivers, batchId));
        drivers = getAvailableDriversAfterAssignment(drivers, totalResult.getAssignedDriverIds());

        // Phase 2: Optimize round-trip rides by vehicle type
        for (Map.Entry<String, List<Ride>> entry : categorization.getRoundTripRidesByVehicleType().entrySet()) {
            totalResult.merge(optimizeRidesForVehicleType(entry.getValue(), drivers, batchId, true, entry.getKey()));
            drivers = getAvailableDriversAfterAssignment(drivers, totalResult.getAssignedDriverIds());
        }

        // Phase 3: Optimize one-way rides by vehicle type
        for (Map.Entry<String, List<Ride>> entry : categorization.getOneWayRidesByVehicleType().entrySet()) {
            totalResult.merge(optimizeRidesForVehicleType(entry.getValue(), drivers, batchId, false, entry.getKey()));
            drivers = getAvailableDriversAfterAssignment(drivers, totalResult.getAssignedDriverIds());
        }

        // Optional relaxed second pass for remaining unassigned rides
        if (Boolean.TRUE.equals(appProps.getOptimizer().isRelaxForUnassigned())) {
            Set<Long> alreadyAssigned = totalResult.getDriverAssignments().values().stream()
                    .flatMap(java.util.Collection::stream)
                    .collect(java.util.stream.Collectors.toSet());
            List<Ride> remaining = rides.stream()
                    .filter(r -> r.getId() != null && !alreadyAssigned.contains(r.getId()))
                    .toList();
            if (!remaining.isEmpty()) {
                log.info("🟡 Relaxed second pass enabled. Attempting to assign {} remaining rides", remaining.size());
                OptimizationResult relaxed = performRelaxedAssignment(remaining, drivers, batchId,
                        appProps.getOptimizer().getRelaxMaxPerDriver());
                totalResult.merge(relaxed);
                drivers = getAvailableDriversAfterAssignment(drivers, totalResult.getAssignedDriverIds());
            }
        }

        createDetailedAuditRecord(rides, totalResult, categorization, batchId);
        logOptimizationResults(batchId, totalResult, rides.size());

        return totalResult;
    }

    /**
     * Safe categorization that handles potential null patients
     */
    private RideCategorization categorizeRides(List<Ride> rides) {
        RideCategorization categorization = new RideCategorization();

        for (Ride ride : rides) {
            try {
                if (ride.getPriority() == Priority.EMERGENCY) {
                    categorization.addEmergencyRide(ride);
                    continue;
                }

                String requiredVehicleType = determineRequiredVehicleType(ride);

                if (isRoundTripRide(ride)) {
                    categorization.addRoundTripRide(requiredVehicleType, ride);
                } else {
                    categorization.addOneWayRide(requiredVehicleType, ride);
                }
            } catch (Exception e) {
                log.warn("Error categorizing ride {}: {}", ride.getId(), e.getMessage());
                // Default to one-way sedan if categorization fails
                categorization.addOneWayRide("sedan", ride);
            }
        }

        logCategorizationResults(categorization);
        return categorization;
    }

    /**
     * Safe vehicle type determination with null safety
     */
    private String determineRequiredVehicleType(Ride ride) {
        try {
            Patient patient = ride.getPatient();

            // First check if ride has explicit vehicle type
            if (ride.getRequiredVehicleType() != null && !ride.getRequiredVehicleType().isEmpty()) {
                return ride.getRequiredVehicleType().toLowerCase();
            }

            // If no patient, default to sedan
            if (patient == null) {
                log.debug("Ride {} has no patient, defaulting to sedan", ride.getId());
                return "sedan";
            }

            // Check patient medical requirements safely
            if (Boolean.TRUE.equals(patient.getRequiresStretcher()) ||
                    patient.getMobilityLevel() == MobilityLevel.STRETCHER) {
                return "stretcher_van";
            }
            if (Boolean.TRUE.equals(patient.getRequiresWheelchair()) ||
                    patient.getMobilityLevel() == MobilityLevel.WHEELCHAIR) {
                return "wheelchair_van";
            }
            if (Boolean.TRUE.equals(patient.getRequiresOxygen())) {
                return "wheelchair_van"; // Oxygen usually requires wheelchair van
            }

            return "sedan";

        } catch (Exception e) {
            log.warn("Error determining vehicle type for ride {}: {}", ride.getId(), e.getMessage());
            return "sedan"; // Safe default
        }
    }

    /**
     * Safe patient capability checking with null safety
     */
    private boolean canDriverHandlePatient(Driver driver, Patient patient) {
        try {
            if (patient == null) return true;

            // Check each requirement safely
            if (Boolean.TRUE.equals(patient.getRequiresWheelchair()) &&
                    !Boolean.TRUE.equals(driver.getWheelchairAccessible())) {
                return false;
            }

            if (Boolean.TRUE.equals(patient.getRequiresStretcher()) &&
                    !Boolean.TRUE.equals(driver.getStretcherCapable())) {
                return false;
            }

            if (Boolean.TRUE.equals(patient.getRequiresOxygen()) &&
                    !Boolean.TRUE.equals(driver.getOxygenEquipped())) {
                return false;
            }

            return true;

        } catch (Exception e) {
            log.warn("Error checking driver-patient compatibility: {}", e.getMessage());
            return false; // Err on the side of caution
        }
    }

    /**
     * Optimize emergency rides with highest priority
     */
    @Transactional
    public OptimizationResult optimizeEmergencyRides(List<Ride> emergencyRides, List<Driver> drivers, String batchId) {
        if (emergencyRides.isEmpty()) {
            return OptimizationResult.empty();
        }

        log.info("🚨 Optimizing {} EMERGENCY rides first", emergencyRides.size());
        OptimizationResult result = new OptimizationResult();
        result.setBatchId(batchId);
        result.setTotalRides(emergencyRides.size());

        for (Ride ride : emergencyRides) {
            try {
                Driver bestDriver = findBestEmergencyDriver(ride, drivers);
                if (bestDriver != null) {
                    assignRideToDriver(ride, bestDriver, bestDriver, batchId, "EMERGENCY_ASSIGNMENT");
                    result.addAssignedRide(bestDriver.getId(), ride.getId());
                    log.info("🚨 Emergency ride {} assigned to driver {}", ride.getId(), bestDriver.getName());
                } else {
                    log.warn("❌ No qualified driver found for emergency ride {}", ride.getId());
                    result.addUnassignedRide(ride.getId(), "No qualified emergency driver available");
                }
            } catch (Exception e) {
                log.error("Error assigning emergency ride {}: {}", ride.getId(), e.getMessage());
                result.addUnassignedRide(ride.getId(), "Error during emergency assignment: " + e.getMessage());
            }
        }

        return result;
    }

    /**
     * Intelligent assignment algorithm with proper error handling
     */
    @Transactional
    public OptimizationResult performIntelligentAssignment(List<Ride> rides, List<Driver> drivers,
                                                           String batchId, boolean isRoundTrip) {
        OptimizationResult result = new OptimizationResult();
        result.setBatchId(batchId);
        result.setTotalRides(rides.size());

        // Sort rides by priority and time
        List<Ride> sortedRides = rides.stream()
                .sorted(Comparator
                        .comparing((Ride r) -> r.getPriority() == Priority.EMERGENCY ? 0 :
                                r.getPriority() == Priority.URGENT ? 1 : 2)
                        .thenComparing(Ride::getPickupTime))
                .collect(Collectors.toList());

        for (Ride ride : sortedRides) {
            try {
                Driver bestDriver = findBestDriverForRide(ride, drivers);

                if (bestDriver != null) {
                    if (isRoundTrip) {
                        assignRideToDriver(ride, bestDriver, bestDriver, batchId, "INTELLIGENT_ROUND_TRIP");
                    } else {
                        Driver dropoffDriver = findBestDriverForDropoff(ride, drivers, bestDriver);
                        assignRideToDriver(ride, bestDriver, dropoffDriver != null ? dropoffDriver : bestDriver,
                                batchId, "INTELLIGENT_ONE_WAY");
                    }
                    result.addAssignedRide(bestDriver.getId(), ride.getId());
                    log.debug("✅ Ride {} assigned to driver {} ({})", ride.getId(), bestDriver.getName(),
                            isRoundTrip ? "round-trip" : "one-way");
                } else {
                    String reason = buildUnassignedReason(ride, drivers);
                    result.addUnassignedRide(ride.getId(), reason);
                    log.warn("❌ Could not assign ride {}. Reason: {}", ride.getId(), reason);
                }
            } catch (Exception e) {
                log.error("Error assigning ride {}: {}", ride.getId(), e.getMessage());
                result.addUnassignedRide(ride.getId(), "Assignment error: " + e.getMessage());
            }
        }

        return result;
    }

    // ========== HELPER METHODS ==========

    private boolean isRoundTripRide(Ride ride) {
        return Boolean.TRUE.equals(ride.getIsRoundTrip()) ||
                (ride.getAppointmentDuration() != null && ride.getAppointmentDuration() <= SHORT_APPOINTMENT_THRESHOLD) ||
                ride.getRideType() == RideType.ROUND_TRIP;
    }

    private OptimizationResult optimizeRidesForVehicleType(List<Ride> rides, List<Driver> drivers,
                                                           String batchId, boolean isRoundTrip, String vehicleType) {
        if (rides.isEmpty()) {
            return OptimizationResult.empty();
        }

        String rideTypeLabel = isRoundTrip ? "round-trip" : "one-way";
        log.info("🔄 Optimizing {} {} rides for vehicle type: {}", rides.size(), rideTypeLabel, vehicleType);

        List<Driver> compatibleDrivers = getDriversForVehicleType(drivers, vehicleType);
        if (compatibleDrivers.isEmpty()) {
            log.warn("❌ No compatible drivers for vehicle type: {}", vehicleType);
            return createUnassignedResult(rides, "No compatible " + vehicleType + " drivers available");
        }

        return performIntelligentAssignment(rides, compatibleDrivers, batchId, isRoundTrip);
    }

    private List<Driver> getDriversForVehicleType(List<Driver> drivers, String vehicleType) {
        return drivers.stream()
                .filter(driver -> isDriverCompatibleWithVehicleType(driver, vehicleType))
                .collect(Collectors.toList());
    }

    private boolean isDriverCompatibleWithVehicleType(Driver driver, String vehicleType) {
        if (vehicleType == null || vehicleType.isEmpty()) return true;

        return switch (vehicleType.toLowerCase()) {
            case "wheelchair_van" -> Boolean.TRUE.equals(driver.getWheelchairAccessible());
            case "stretcher_van" -> Boolean.TRUE.equals(driver.getStretcherCapable());
            case "ambulance" -> Boolean.TRUE.equals(driver.getStretcherCapable()) &&
                    Boolean.TRUE.equals(driver.getOxygenEquipped());
            case "van" -> driver.getVehicleType() != null && driver.getVehicleType().name().contains("VAN");
            case "sedan" -> true;
            default -> {
                log.warn("Unknown vehicle type: {}, allowing any driver", vehicleType);
                yield true;
            }
        };
    }

    @Transactional(readOnly = true)
    public List<Driver> getQualifiedDrivers() {
        Long org = currentOrgId();
        return driverRepository.findByOrgIdAndActiveTrueAndIsTrainingCompleteTrue(org).stream()
                .filter(driver -> !isLicenseExpiringSoon(driver))
                .collect(Collectors.toList());
    }

    private boolean isLicenseExpiringSoon(Driver driver) {
        LocalDate thirtyDaysFromNow = LocalDate.now().plusDays(30);
        return (driver.getDriversLicenseExpiry() != null && driver.getDriversLicenseExpiry().isBefore(thirtyDaysFromNow)) ||
                (driver.getMedicalTransportLicenseExpiry() != null && driver.getMedicalTransportLicenseExpiry().isBefore(thirtyDaysFromNow)) ||
                (driver.getInsuranceExpiry() != null && driver.getInsuranceExpiry().isBefore(thirtyDaysFromNow));
    }

    private Driver findBestEmergencyDriver(Ride ride, List<Driver> drivers) {
        return drivers.stream()
                .filter(driver -> canDriverHandlePatient(driver, ride.getPatient()))
                .filter(driver -> hasRequiredSkills(driver, ride.getRequiredSkills()))
                .min(Comparator.comparingDouble(driver -> calculateDistanceToPickup(driver, ride)))
                .orElse(null);
    }

    private Driver findBestDriverForRide(Ride ride, List<Driver> drivers) {
        return drivers.stream()
                .filter(driver -> canDriverHandlePatient(driver, ride.getPatient()))
                .filter(driver -> hasRequiredSkills(driver, ride.getRequiredSkills()))
                .filter(driver -> isDriverAvailableForRide(driver, ride))
                .filter(driver -> calculateDistanceToPickup(driver, ride) <= appProps.getOptimizer().getMaxPickupDistanceKm())
                .min(Comparator.comparingDouble(driver -> calculateDriverScore(driver, ride)))
                .orElse(null);
    }

    private Driver findBestDriverForDropoff(Ride ride, List<Driver> drivers, Driver pickupDriver) {
        return drivers.stream()
                .filter(driver -> !driver.equals(pickupDriver)) // Different driver for dropoff
                .filter(driver -> canDriverHandlePatient(driver, ride.getPatient()))
                .filter(driver -> isDriverAvailableForDropoff(driver, ride))
                .min(Comparator.comparingDouble(driver -> calculateDropoffScore(driver, ride)))
                .orElse(null);
    }

    private double calculateDriverScore(Driver driver, Ride ride) {
        double score = 0.0;

        // Distance factor (lower is better)
        double distance = calculateDistanceToPickup(driver, ride);
        score += distance * 100; // Weight distance heavily

        // Bonus for being within preferred distance
        if (distance <= PREFERRED_PICKUP_DISTANCE_KM) {
            score -= 200;
        }

        // Capability bonus (exact match is better)
        if (hasExactVehicleMatch(driver, ride)) {
            score -= 50;
        }

        // Experience bonus
        if (driver.getMaxDailyRides() != null) {
            score -= driver.getMaxDailyRides() * 5;
        }

        // Training completion bonus
        if (Boolean.TRUE.equals(driver.getIsTrainingComplete())) {
            score -= 25;
        }

        return score;
    }

    private double calculateDropoffScore(Driver driver, Ride ride) {
        // For dropoff, prioritize drivers closer to the dropoff location
        if (ride.getDropoffLocation() != null && ride.getDropoffLocation().isValid()) {
            return calculateDistance(
                    driver.getBaseLat(), driver.getBaseLng(),
                    ride.getDropoffLocation().getLatitude(), ride.getDropoffLocation().getLongitude()
            );
        }
        return 999.0; // High score if no valid dropoff location
    }

    private boolean hasExactVehicleMatch(Driver driver, Ride ride) {
        String requiredType = determineRequiredVehicleType(ride);
        return switch (requiredType.toLowerCase()) {
            case "wheelchair_van" -> Boolean.TRUE.equals(driver.getWheelchairAccessible());
            case "stretcher_van" -> Boolean.TRUE.equals(driver.getStretcherCapable());
            case "ambulance" -> Boolean.TRUE.equals(driver.getStretcherCapable()) &&
                    Boolean.TRUE.equals(driver.getOxygenEquipped());
            default -> true;
        };
    }

    private boolean isDriverAvailableForRide(Driver driver, Ride ride) {
        return driver.getActive() && Boolean.TRUE.equals(driver.getIsTrainingComplete());
    }

    private boolean isDriverAvailableForDropoff(Driver driver, Ride ride) {
        return isDriverAvailableForRide(driver, ride);
    }

    private boolean hasRequiredSkills(Driver driver, List<String> requiredSkills) {
        if (requiredSkills == null || requiredSkills.isEmpty()) return true;
        if (driver.getSkills() == null) return false;

        return requiredSkills.stream()
                .allMatch(skill -> Boolean.TRUE.equals(driver.getSkills().get(skill)));
    }

    private double calculateDistanceToPickup(Driver driver, Ride ride) {
        if (ride.getPickupLocation() == null || !ride.getPickupLocation().isValid()) {
            return Double.MAX_VALUE;
        }

        return calculateDistance(
                driver.getBaseLat(), driver.getBaseLng(),
                ride.getPickupLocation().getLatitude(), ride.getPickupLocation().getLongitude()
        );
    }

    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lng1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lng2);

        double dlat = lat2Rad - lat1Rad;
        double dlon = lon2Rad - lon1Rad;

        double a = Math.sin(dlat/2) * Math.sin(dlat/2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) * Math.sin(dlon/2) * Math.sin(dlon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        return EARTH_RADIUS_KM * c;
    }

    private List<Driver> getAvailableDriversAfterAssignment(List<Driver> allDrivers, Set<Long> assignedDriverIds) {
        return allDrivers.stream()
                .filter(driver -> !assignedDriverIds.contains(driver.getId()))
                .collect(Collectors.toList());
    }

    private OptimizationResult performRelaxedAssignment(List<Ride> rides, List<Driver> drivers, String batchId, int maxPerDriver) {
        OptimizationResult res = new OptimizationResult();
        res.setBatchId(batchId);
        res.setTotalRides(rides.size());

        Map<Long, Integer> driverRelaxedCounts = new HashMap<>();

        for (Ride ride : rides) {
            try {
                Driver best = drivers.stream()
                        .filter(d -> canDriverHandlePatient(d, ride.getPatient()))
                        .filter(d -> hasRequiredSkills(d, ride.getRequiredSkills()))
                        .filter(d -> isDriverAvailableForRide(d, ride))
                        // Ignore MAX_PICKUP_DISTANCE_KM constraint in relaxed mode
                        .min(Comparator.comparingDouble(d -> calculateDistanceToPickup(d, ride)))
                        .orElse(null);

                if (best != null) {
                    int used = driverRelaxedCounts.getOrDefault(best.getId(), 0);
                    if (used < Math.max(0, maxPerDriver)) {
                        assignRideToDriver(ride, best, best, batchId, "RELAXED_LONG_DEADHEAD");
                        res.addAssignedRide(best.getId(), ride.getId());
                        driverRelaxedCounts.put(best.getId(), used + 1);
                        log.info("🟡 Relaxed assign ride {} -> driver {} (deadhead ~{:.1f}km)",
                                ride.getId(), best.getName(), calculateDistanceToPickup(best, ride));
                    } else {
                        res.addUnassignedRide(ride.getId(), "Relaxed cap reached for driver");
                    }
                } else {
                    res.addUnassignedRide(ride.getId(), "No feasible driver under relaxed rules");
                }
            } catch (Exception e) {
                log.warn("Relaxed assignment error for ride {}: {}", ride.getId(), e.getMessage());
                res.addUnassignedRide(ride.getId(), "Relaxed assignment error: " + e.getMessage());
            }
        }
        return res;
    }

    private String buildUnassignedReason(Ride ride, List<Driver> drivers) {
        List<String> reasons = new ArrayList<>();
        long compatibleByPatient = drivers.stream().filter(d -> canDriverHandlePatient(d, ride.getPatient())).count();
        if (compatibleByPatient == 0) reasons.add("No driver matches patient medical needs");

        long withinShift = drivers.stream().filter(d -> isDriverAvailableForRide(d, ride)).count();
        if (withinShift == 0) reasons.add("No driver available (inactive/training/shift)");

        long withinDistance = drivers.stream()
                .filter(d -> calculateDistanceToPickup(d, ride) <= MAX_PICKUP_DISTANCE_KM)
                .count();
        if (withinDistance == 0) reasons.add("All drivers too far from pickup");

        if (reasons.isEmpty()) reasons.add("No compatible driver available");
        return String.join("; ", reasons);
    }

    @Transactional
    public void assignRideToDriver(Ride ride, Driver pickupDriver, Driver dropoffDriver, String batchId, String method) {
        ride.setPickupDriver(pickupDriver);
        ride.setDropoffDriver(dropoffDriver);
        ride.setDriver(pickupDriver); // Backward compatibility
        updateRideAssignment(ride, batchId, method);
    }

    @Transactional
    public void updateRideAssignment(Ride ride, String batchId, String method) {
        ride.setStatus(RideStatus.ASSIGNED);
        ride.setAssignedAt(LocalDateTime.now());
        ride.setAssignedBy("ENHANCED_MEDICAL_OPTIMIZER");
        ride.setOptimizationBatchId(batchId);
//        ride.setAssignment(method);
        rideRepository.save(ride);
    }

    @Transactional
    public OptimizationResult performIntelligentFallback(List<Ride> rides, List<Driver> drivers, String batchId) {
        log.warn("⚠️ Running intelligent medical transport fallback for {} rides", rides.size());
        return performIntelligentAssignment(rides, drivers, batchId, false);
    }

    private OptimizationResult createUnassignedResult(List<Ride> rides, String reason) {
        OptimizationResult result = new OptimizationResult();
        result.setTotalRides(rides.size());
        rides.forEach(ride -> result.addUnassignedRide(ride.getId(), reason));
        return result;
    }

    private String generateBatchId() {
        return "MEDICAL_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) +
                "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private void logCategorizationResults(RideCategorization categorization) {
        log.info("📊 Ride categorization complete:");
        log.info("   🚨 Emergency: {}", categorization.getEmergencyRides().size());
        log.info("   🔄 Round-trip: {}", categorization.getRoundTripRideCount());
        log.info("   ♿ Wheelchair: {}", categorization.getWheelchairRideCount());
        log.info("   🏥 Stretcher: {}", categorization.getStretcherRideCount());
    }

    private void logOptimizationResults(String batchId, OptimizationResult result, int totalRides) {
        log.info("✅ Enhanced medical transport optimization complete");
        log.info("📈 Batch: {}, Success Rate: {:.1f}%, Assigned: {}/{}",
                batchId, result.getSuccessRate(), result.getAssignedRideCount(), totalRides);
    }

    @Transactional
    public void createDetailedAuditRecord(List<Ride> rides, OptimizationResult result,
                                          RideCategorization categorization, String batchId) {
        try {
            AssignmentAudit audit = new AssignmentAudit();
            audit.setAssignmentTime(LocalDateTime.now());
            Long org = currentOrgId();
            if (org != null) audit.setOrgId(org);
            audit.setBatchId(batchId);
            audit.setAssignmentDate(rides.isEmpty() ? LocalDate.now() : rides.get(0).getPickupTime().toLocalDate());
            audit.setTotalRides(rides.size());
            audit.setAssignedRides(result.getAssignedRideCount());
            audit.setUnassignedRides(rides.size() - result.getAssignedRideCount());
//            audit.setAssignedDriverCount(result.getAssignedDriverCount());
            audit.setSuccessRate(result.getSuccessRate());
            audit.setTriggeredBy("ENHANCED_MEDICAL_OPTIMIZER");
            audit.setOptimizationStrategy("Intelligent fallback algorithm with medical transport constraints");

            // Medical transport specific statistics
            audit.setWheelchairRides(categorization.getWheelchairRideCount());
            audit.setStretcherRides(categorization.getStretcherRideCount());
            audit.setRoundTripRides(categorization.getRoundTripRideCount());
            audit.setEmergencyRides(categorization.getEmergencyRides().size());

            audit.setRideAssignmentsSummary(result.getDriverAssignments());
            audit.setUnassignedReasons(result.getUnassignedReasons());

            assignmentAuditRepository.save(audit);
            log.info("📊 Enhanced audit record created for batch {}", batchId);
        } catch (Exception e) {
            log.error("Failed to create enhanced audit record for batch {}: {}", batchId, e.getMessage(), e);
        }
    }

    // Supporting Classes
    public static class OptimizationResult {
        private String batchId;
        private int totalRides;
        private Map<Long, List<Long>> driverAssignments = new HashMap<>();
        private Map<Long, String> unassignedReasons = new HashMap<>();
        private LocalDateTime optimizationTime = LocalDateTime.now();

        public static OptimizationResult create(String batchId, int totalRides) {
            OptimizationResult result = new OptimizationResult();
            result.setBatchId(batchId);
            result.setTotalRides(totalRides);
            return result;
        }

        public static OptimizationResult empty() {
            return new OptimizationResult();
        }

        public void addAssignedRide(Long driverId, Long rideId) {
            driverAssignments.computeIfAbsent(driverId, k -> new ArrayList<>()).add(rideId);
        }

        public void addUnassignedRide(Long rideId, String reason) {
            unassignedReasons.put(rideId, reason);
        }

        public void merge(OptimizationResult other) {
            if (other != null) {
                for (Map.Entry<Long, List<Long>> entry : other.driverAssignments.entrySet()) {
                    this.driverAssignments.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).addAll(entry.getValue());
                }
                this.unassignedReasons.putAll(other.unassignedReasons);
            }
        }

        public int getAssignedRideCount() {
            return driverAssignments.values().stream().mapToInt(List::size).sum();
        }

        public int getAssignedDriverCount() {
            return driverAssignments.size();
        }

        public Set<Long> getAssignedDriverIds() {
            return driverAssignments.keySet();
        }

        public double getSuccessRate() {
            return totalRides > 0 ? (getAssignedRideCount() * 100.0) / totalRides : 0.0;
        }

        // Getters and setters
        public String getBatchId() { return batchId; }
        public void setBatchId(String batchId) { this.batchId = batchId; }
        public int getTotalRides() { return totalRides; }
        public void setTotalRides(int totalRides) { this.totalRides = totalRides; }
        public Map<Long, List<Long>> getDriverAssignments() { return driverAssignments; }
        public Map<Long, String> getUnassignedReasons() { return unassignedReasons; }
        public LocalDateTime getOptimizationTime() { return optimizationTime; }
    }

    public static class RideCategorization {
        private List<Ride> emergencyRides = new ArrayList<>();
        private Map<String, List<Ride>> roundTripRidesByVehicleType = new HashMap<>();
        private Map<String, List<Ride>> oneWayRidesByVehicleType = new HashMap<>();

        public void addEmergencyRide(Ride ride) {
            emergencyRides.add(ride);
        }

        public void addRoundTripRide(String vehicleType, Ride ride) {
            roundTripRidesByVehicleType.computeIfAbsent(vehicleType, k -> new ArrayList<>()).add(ride);
        }

        public void addOneWayRide(String vehicleType, Ride ride) {
            oneWayRidesByVehicleType.computeIfAbsent(vehicleType, k -> new ArrayList<>()).add(ride);
        }

        public int getWheelchairRideCount() {
            return getRideCountForVehicleType("wheelchair_van");
        }

        public int getStretcherRideCount() {
            return getRideCountForVehicleType("stretcher_van");
        }

        public int getRoundTripRideCount() {
            return roundTripRidesByVehicleType.values().stream().mapToInt(List::size).sum();
        }

        private int getRideCountForVehicleType(String vehicleType) {
            return roundTripRidesByVehicleType.getOrDefault(vehicleType, Collections.emptyList()).size() +
                    oneWayRidesByVehicleType.getOrDefault(vehicleType, Collections.emptyList()).size();
        }

        // Getters
        public List<Ride> getEmergencyRides() { return emergencyRides; }
        public Map<String, List<Ride>> getRoundTripRidesByVehicleType() { return roundTripRidesByVehicleType; }
        public Map<String, List<Ride>> getOneWayRidesByVehicleType() { return oneWayRidesByVehicleType; }
    }
}