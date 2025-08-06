package com.mediroute.service.ride;

import com.google.ortools.constraintsolver.*;
import com.google.protobuf.Duration;
import com.mediroute.dto.*;
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
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnhancedMedicalTransportOptimizer {

    private final RideRepository rideRepository;
    private final DriverRepository driverRepository;
    private final OsrmDistanceService distanceService;
    private final AssignmentAuditRepository assignmentAuditRepository;

    // Medical transport constants
    private static final int DEFAULT_TIME_WINDOW_MINUTES = 5;
    private static final int SHORT_APPOINTMENT_THRESHOLD = 15;
    private static final long VEHICLE_MISMATCH_PENALTY = 1000000L;
    private static final long SKILL_MISMATCH_PENALTY = 250000L;
    private static final long PRIORITY_BONUS = 100000L;
    private static final long DISTANCE_MULTIPLIER = 1000L;
    private static final int OPTIMIZATION_TIMEOUT_SECONDS = 45;

    static {
        try {
            System.loadLibrary("jniortools");
            log.info("✅ OR-Tools library loaded successfully");
        } catch (UnsatisfiedLinkError e) {
            log.error("❌ Failed to load OR-Tools library", e);
            throw new RuntimeException("OR-Tools library not available", e);
        }
    }

    /**
     * Main optimization entry point for medical transport
     */
    public OptimizationResult optimizeSchedule(List<Ride> rides) {
        if (rides == null || rides.isEmpty()) {
            log.warn("⚠️ No rides to optimize");
            return OptimizationResult.empty();
        }

        List<Driver> availableDrivers = getQualifiedDrivers();
        if (availableDrivers.isEmpty()) {
            log.warn("⚠️ No qualified drivers available for medical transport");
            return OptimizationResult.empty();
        }

        String batchId = generateBatchId();
        log.info("🏥 Starting enhanced medical transport optimization for {} rides with {} drivers (Batch: {})",
                rides.size(), availableDrivers.size(), batchId);

        try {
            return performMedicalTransportOptimization(rides, availableDrivers, batchId);
        } catch (Exception e) {
            log.error("❌ Enhanced optimization failed: {}", e.getMessage(), e);
            return performIntelligentFallback(rides, availableDrivers, batchId);
        }
    }

    /**
     * Core medical transport optimization with vehicle matching and flexible driver assignment
     */
    private OptimizationResult performMedicalTransportOptimization(List<Ride> rides, List<Driver> drivers, String batchId) {
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

        createDetailedAuditRecord(rides, totalResult, categorization, batchId);
        logOptimizationResults(batchId, totalResult, rides.size());

        return totalResult;
    }

    /**
     * Categorize rides by medical requirements, appointment type, and priority
     */
    private RideCategorization categorizeRides(List<Ride> rides) {
        RideCategorization categorization = new RideCategorization();

        for (Ride ride : rides) {
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
        }

        logCategorizationResults(categorization);
        return categorization;
    }

    /**
     * Optimize emergency rides with highest priority
     */
    private OptimizationResult optimizeEmergencyRides(List<Ride> emergencyRides, List<Driver> drivers, String batchId) {
        if (emergencyRides.isEmpty()) {
            return OptimizationResult.empty();
        }

        log.info("🚨 Optimizing {} EMERGENCY rides first", emergencyRides.size());
        OptimizationResult result = new OptimizationResult();

        for (Ride ride : emergencyRides) {
            Driver bestDriver = findBestEmergencyDriver(ride, drivers);
            if (bestDriver != null) {
                assignRideToDriver(ride, bestDriver, bestDriver, batchId, "EMERGENCY_ASSIGNMENT");
                result.addAssignedRide(bestDriver.getId(), ride.getId());
                log.info("🚨 Emergency ride {} assigned to driver {}", ride.getId(), bestDriver.getName());
            } else {
                log.warn("❌ No qualified driver found for emergency ride {}", ride.getId());
                result.addUnassignedRide(ride.getId(), "No qualified emergency driver available");
            }
        }

        return result;
    }

    /**
     * Unified method to optimize rides for a specific vehicle type
     */
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

        try {
            return optimizeWithORTools(rides, compatibleDrivers, batchId, isRoundTrip);
        } catch (Exception e) {
            log.warn("❌ OR-Tools optimization failed for {} rides: {}", rideTypeLabel, e.getMessage());
            return performSimpleFallback(rides, compatibleDrivers, batchId);
        }
    }

    /**
     * Core OR-Tools optimization logic
     */
    private OptimizationResult optimizeWithORTools(List<Ride> rides, List<Driver> drivers,
                                                   String batchId, boolean isRoundTrip) {
        LocationMapping locationMapping = isRoundTrip ?
                buildRoundTripLocationMapping(rides, drivers) :
                buildOneWayLocationMapping(rides, drivers);

        double[][] costMatrix = distanceService.getDistanceMatrix(locationMapping.getLocationStrings());
        if (costMatrix == null || costMatrix.length == 0) {
            throw new RuntimeException("Failed to get distance matrix");
        }

        // Create OR-Tools model
        RoutingIndexManager manager = new RoutingIndexManager(
                locationMapping.getTotalNodes(), drivers.size(),
                IntStream.range(0, drivers.size()).toArray(),
                IntStream.range(0, drivers.size()).toArray()
        );

        RoutingModel routing = new RoutingModel(manager);

        // Add constraints and callbacks
        addDistanceCallback(routing, manager, costMatrix, locationMapping, drivers, rides);
        addCapacityConstraints(routing, manager, drivers, locationMapping, isRoundTrip);
        addTimeWindowConstraints(routing, manager, locationMapping, rides);
        addDisjunctionConstraints(routing, manager, locationMapping, rides);

        // Solve and apply solution
        Assignment solution = solveWithTimeLimit(routing, OPTIMIZATION_TIMEOUT_SECONDS);
        if (solution != null) {
            return applySolution(solution, routing, manager, locationMapping, drivers, rides, batchId, isRoundTrip);
        } else {
            throw new RuntimeException("No OR-Tools solution found");
        }
    }

    /**
     * Enhanced distance callback with medical transport penalties
     */
    private void addDistanceCallback(RoutingModel routing, RoutingIndexManager manager,
                                     double[][] costMatrix, LocationMapping locationMapping,
                                     List<Driver> drivers, List<Ride> rides) {

        int transitCallbackIndex = routing.registerTransitCallback((fromIdx, toIdx) -> {
            int fromNode = manager.indexToNode((int) fromIdx);
            int toNode = manager.indexToNode((int) toIdx);

            long baseCost = Math.round(costMatrix[fromNode][toNode] * DISTANCE_MULTIPLIER);
            String fromEntity = locationMapping.getEntityForIndex(fromNode);
            String toEntity = locationMapping.getEntityForIndex(toNode);

            long penalties = calculateVehicleCompatibilityPenalty(fromEntity, toEntity, drivers, rides) +
                    calculateSkillMatchingPenalty(fromEntity, toEntity, drivers, rides) +
                    calculatePriorityBonus(toEntity, rides);

            return baseCost + penalties;
        });

        routing.setArcCostEvaluatorOfAllVehicles(transitCallbackIndex);
    }

    /**
     * Add capacity constraints based on ride type
     */
    private void addCapacityConstraints(RoutingModel routing, RoutingIndexManager manager,
                                        List<Driver> drivers, LocationMapping locationMapping, boolean isRoundTrip) {
        int[] demands = new int[locationMapping.getTotalNodes()];

        // Set demands based on ride type
        for (int i = 0; i < locationMapping.getTotalNodes(); i++) {
            String entity = locationMapping.getEntityForIndex(i);
            if (entity != null) {
                if (isRoundTrip && entity.startsWith("ROUND_TRIP:")) {
                    demands[i] = 1; // Pick up patient
                } else if (!isRoundTrip) {
                    if (entity.startsWith("PICKUP:")) {
                        demands[i] = 1; // Pick up patient
                    } else if (entity.startsWith("DROPOFF:")) {
                        demands[i] = -1; // Drop off patient
                    }
                }
            }
        }

        long[] capacities = drivers.stream()
                .mapToLong(driver -> Math.min(
                        Optional.ofNullable(driver.getVehicleCapacity()).orElse(4),
                        Optional.ofNullable(driver.getMaxDailyRides()).orElse(8)
                ))
                .toArray();

        int demandCallbackIdx = routing.registerUnaryTransitCallback(fromIdx -> {
            int fromNode = manager.indexToNode((int) fromIdx);
            return demands[fromNode];
        });

        routing.addDimensionWithVehicleCapacity(demandCallbackIdx, 0, capacities, true, "Capacity");
    }

    /**
     * Add time window constraints for medical appointments
     */
    private void addTimeWindowConstraints(RoutingModel routing, RoutingIndexManager manager,
                                          LocationMapping locationMapping, List<Ride> rides) {
        int timeCallbackIdx = routing.registerTransitCallback((fromIdx, toIdx) -> 600); // 10 min average

        routing.addDimension(timeCallbackIdx, 7200, 86400, false, "Time");
        RoutingDimension timeDimension = routing.getMutableDimension("Time");

        // Set time windows for each node
        for (int i = 0; i < locationMapping.getTotalNodes(); i++) {
            String entity = locationMapping.getEntityForIndex(i);
            long[] timeWindow = getTimeWindowForEntity(entity, rides);

            long nodeIndex = manager.nodeToIndex(i);
            timeDimension.cumulVar(nodeIndex).setRange(timeWindow[0], timeWindow[1]);
        }
    }

    /**
     * Add disjunction constraints (allow skipping rides with penalties)
     */
    private void addDisjunctionConstraints(RoutingModel routing, RoutingIndexManager manager,
                                           LocationMapping locationMapping, List<Ride> rides) {
        for (int i = 0; i < locationMapping.getTotalNodes(); i++) {
            String entity = locationMapping.getEntityForIndex(i);
            if (entity != null && isRideEntity(entity)) {
                long nodeIndex = manager.nodeToIndex(i);
                long penalty = calculateSkippingPenalty(entity, rides);
                routing.addDisjunction(new long[]{nodeIndex}, penalty);
            }
        }
    }

    /**
     * Apply optimization solution based on ride type
     */
    private OptimizationResult applySolution(Assignment solution, RoutingModel routing, RoutingIndexManager manager,
                                             LocationMapping locationMapping, List<Driver> drivers, List<Ride> rides,
                                             String batchId, boolean isRoundTrip) {
        OptimizationResult result = new OptimizationResult();

        for (int driverIdx = 0; driverIdx < drivers.size(); driverIdx++) {
            Driver driver = drivers.get(driverIdx);
            long routeIndex = routing.start(driverIdx);

            while (!routing.isEnd(routeIndex)) {
                int nodeIndex = manager.indexToNode((int) routeIndex);
                String entity = locationMapping.getEntityForIndex(nodeIndex);

                processRouteNode(entity, driver, rides, batchId, result, isRoundTrip);
                routeIndex = solution.value(routing.nextVar(routeIndex));
            }
        }

        return result;
    }

    /**
     * Process a single route node and assign rides accordingly
     */
    private void processRouteNode(String entity, Driver driver, List<Ride> rides,
                                  String batchId, OptimizationResult result, boolean isRoundTrip) {
        if (entity == null || !isRideEntity(entity)) return;

        Long rideId = extractRideIdFromEntity(entity);
        if (rideId == null) return;

        Ride ride = findRideById(rides, rideId);
        if (ride == null) return;

        if (isRoundTrip && entity.startsWith("ROUND_TRIP:")) {
            assignRideToDriver(ride, driver, driver, batchId, "ROUND_TRIP_OPTIMIZATION");
            result.addAssignedRide(driver.getId(), rideId);
            log.debug("✅ Round-trip ride {} assigned to driver {}", rideId, driver.getName());
        } else if (!isRoundTrip) {
            if (entity.startsWith("PICKUP:")) {
                ride.setPickupDriver(driver);
            } else if (entity.startsWith("DROPOFF:")) {
                ride.setDropoffDriver(driver);
            }
            updateRideAssignment(ride, batchId, "ONE_WAY_OPTIMIZATION");
            result.addAssignedRide(driver.getId(), rideId);
            log.debug("✅ {} for ride {} assigned to driver {}",
                    entity.split(":")[0], rideId, driver.getName());
        }
    }

    // ============================
    // Penalty and Bonus Calculations
    // ============================

    private long calculateVehicleCompatibilityPenalty(String fromEntity, String toEntity,
                                                      List<Driver> drivers, List<Ride> rides) {
        if (!isRideEntity(toEntity)) return 0L;

        Long rideId = extractRideIdFromEntity(toEntity);
        Ride ride = findRideById(rides, rideId);
        Driver driver = getDriverFromEntity(fromEntity, drivers);

        if (ride == null || ride.getPatient() == null || driver == null) return 0L;

        return canDriverHandlePatient(driver, ride.getPatient()) ? 0L : VEHICLE_MISMATCH_PENALTY;
    }

    private long calculateSkillMatchingPenalty(String fromEntity, String toEntity,
                                               List<Driver> drivers, List<Ride> rides) {
        if (!isRideEntity(toEntity)) return 0L;

        Long rideId = extractRideIdFromEntity(toEntity);
        Ride ride = findRideById(rides, rideId);
        Driver driver = getDriverFromEntity(fromEntity, drivers);

        if (ride == null || ride.getRequiredSkills() == null || ride.getRequiredSkills().isEmpty() ||
                driver == null || driver.getSkills() == null) return 0L;

        boolean hasAllSkills = ride.getRequiredSkills().stream()
                .allMatch(skill -> Boolean.TRUE.equals(driver.getSkills().get(skill)));

        return hasAllSkills ? 0L : SKILL_MISMATCH_PENALTY;
    }

    private long calculatePriorityBonus(String toEntity, List<Ride> rides) {
        if (!isRideEntity(toEntity)) return 0L;

        Long rideId = extractRideIdFromEntity(toEntity);
        Ride ride = findRideById(rides, rideId);

        if (ride == null || ride.getPriority() == null) return 0L;

        return switch (ride.getPriority()) {
            case EMERGENCY -> -PRIORITY_BONUS * 3;
            case URGENT -> -PRIORITY_BONUS;
            case ROUTINE -> 0L;
        };
    }

    private long calculateSkippingPenalty(String entity, List<Ride> rides) {
        Long rideId = extractRideIdFromEntity(entity);
        Ride ride = findRideById(rides, rideId);

        if (ride == null) return 500000L;

        long basePenalty = switch (ride.getPriority()) {
            case EMERGENCY -> 2000000L;
            case URGENT -> 1000000L;
            case ROUTINE -> 200000L;
        };

        if (ride.getPatient() != null && ride.getPatient().hasHighMobilityNeeds()) {
            basePenalty += 300000L;
        }

        return basePenalty;
    }

    // ============================
    // Helper Methods
    // ============================

    private String determineRequiredVehicleType(Ride ride) {
        Patient patient = ride.getPatient();
        if (patient == null) return "sedan";

        if (ride.getRequiredVehicleType() != null && !ride.getRequiredVehicleType().isEmpty()) {
            return ride.getRequiredVehicleType();
        }

        if (Boolean.TRUE.equals(patient.getRequiresStretcher()) ||
                patient.getMobilityLevel() == MobilityLevel.STRETCHER) {
            return "stretcher_van";
        }
        if (Boolean.TRUE.equals(patient.getRequiresWheelchair()) ||
                patient.getMobilityLevel() == MobilityLevel.WHEELCHAIR) {
            return "wheelchair_van";
        }
        if (Boolean.TRUE.equals(patient.getRequiresOxygen())) {
            return "wheelchair_van";
        }

        return "sedan";
    }

    private boolean isRoundTripRide(Ride ride) {
        return Boolean.TRUE.equals(ride.getIsRoundTrip()) ||
                (ride.getAppointmentDuration() != null && ride.getAppointmentDuration() <= SHORT_APPOINTMENT_THRESHOLD) ||
                ride.getRideType() == RideType.ROUND_TRIP;
    }

    private boolean canDriverHandlePatient(Driver driver, Patient patient) {
        if (patient == null) return true;

        return !(Boolean.TRUE.equals(patient.getRequiresWheelchair()) && !Boolean.TRUE.equals(driver.getWheelchairAccessible())) &&
                !(Boolean.TRUE.equals(patient.getRequiresStretcher()) && !Boolean.TRUE.equals(driver.getStretcherCapable())) &&
                !(Boolean.TRUE.equals(patient.getRequiresOxygen()) && !Boolean.TRUE.equals(driver.getOxygenEquipped())) &&
                !(patient.getMobilityLevel() == MobilityLevel.STRETCHER && !Boolean.TRUE.equals(driver.getStretcherCapable())) &&
                !(patient.getMobilityLevel() == MobilityLevel.WHEELCHAIR && !Boolean.TRUE.equals(driver.getWheelchairAccessible()));
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
            case "van" -> driver.getVehicleType().name().contains("VAN");
            case "sedan" -> true;
            default -> {
                log.warn("Unknown vehicle type: {}, allowing any driver", vehicleType);
                yield true;
            }
        };
    }

    private List<Driver> getQualifiedDrivers() {
        return driverRepository.findByActiveTrue().stream()
                .filter(driver -> Boolean.TRUE.equals(driver.getIsTrainingComplete()))
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

    private boolean hasRequiredSkills(Driver driver, List<String> requiredSkills) {
        if (requiredSkills == null || requiredSkills.isEmpty()) return true;
        if (driver.getSkills() == null) return false;

        return requiredSkills.stream()
                .allMatch(skill -> Boolean.TRUE.equals(driver.getSkills().get(skill)));
    }

    private double calculateDistanceToPickup(Driver driver, Ride ride) {
        double lat1 = Math.toRadians(driver.getBaseLat());
        double lon1 = Math.toRadians(driver.getBaseLng());
        double lat2 = Math.toRadians(ride.getPickupLat());
        double lon2 = Math.toRadians(ride.getPickupLng());

        double dlat = lat2 - lat1;
        double dlon = lon2 - lon1;

        double a = Math.sin(dlat/2) * Math.sin(dlat/2) +
                Math.cos(lat1) * Math.cos(lat2) * Math.sin(dlon/2) * Math.sin(dlon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        return 6371 * c; // Earth's radius in kilometers
    }

    private List<Driver> getAvailableDriversAfterAssignment(List<Driver> allDrivers, Set<Long> assignedDriverIds) {
        return allDrivers.stream()
                .filter(driver -> !assignedDriverIds.contains(driver.getId()))
                .collect(Collectors.toList());
    }

    private long[] getTimeWindowForEntity(String entity, List<Ride> rides) {
        if (entity == null || !isRideEntity(entity)) {
            return new long[]{0, 86400}; // Default 24-hour window
        }

        Long rideId = extractRideIdFromEntity(entity);
        Ride ride = findRideById(rides, rideId);
        if (ride == null) return new long[]{0, 86400};

        if (entity.startsWith("PICKUP:") || entity.startsWith("ROUND_TRIP:")) {
            if (ride.getPickupWindowStart() != null && ride.getPickupWindowEnd() != null) {
                return new long[]{
                        ride.getPickupWindowStart().toEpochSecond(ZoneOffset.UTC),
                        ride.getPickupWindowEnd().toEpochSecond(ZoneOffset.UTC)
                };
            }
        } else if (entity.startsWith("DROPOFF:")) {
            if (ride.getDropoffWindowStart() != null && ride.getDropoffWindowEnd() != null) {
                return new long[]{
                        ride.getDropoffWindowStart().toEpochSecond(ZoneOffset.UTC),
                        ride.getDropoffWindowEnd().toEpochSecond(ZoneOffset.UTC)
                };
            }
        }

        return new long[]{0, 86400};
    }

    // ============================
    // Location Mapping Builders
    // ============================

    private LocationMapping buildRoundTripLocationMapping(List<Ride> rides, List<Driver> drivers) {
        LocationMapping mapping = new LocationMapping();

        for (Driver driver : drivers) {
            mapping.addLocation(driver.getBaseLng() + "," + driver.getBaseLat(), "DRIVER:" + driver.getId());
        }

        for (Ride ride : rides) {
            mapping.addLocation(ride.getPickupLng() + "," + ride.getPickupLat(), "ROUND_TRIP:" + ride.getId());
        }

        return mapping;
    }

    private LocationMapping buildOneWayLocationMapping(List<Ride> rides, List<Driver> drivers) {
        LocationMapping mapping = new LocationMapping();

        for (Driver driver : drivers) {
            mapping.addLocation(driver.getBaseLng() + "," + driver.getBaseLat(), "DRIVER:" + driver.getId());
        }

        for (Ride ride : rides) {
            mapping.addLocation(ride.getPickupLng() + "," + ride.getPickupLat(), "PICKUP:" + ride.getId());
            mapping.addLocation(ride.getDropoffLng() + "," + ride.getDropoffLat(), "DROPOFF:" + ride.getId());
        }

        return mapping;
    }

    // ============================
    // Utility Methods
    // ============================

    private boolean isRideEntity(String entity) {
        return entity != null && (entity.startsWith("PICKUP:") || entity.startsWith("DROPOFF:") || entity.startsWith("ROUND_TRIP:"));
    }

    private Long extractRideIdFromEntity(String entity) {
        if (entity == null) return null;

        try {
            String[] parts = entity.split(":");
            return parts.length == 2 ? Long.parseLong(parts[1]) : null;
        } catch (NumberFormatException e) {
            log.warn("Invalid ride ID in entity: {}", entity);
            return null;
        }
    }

    private Ride findRideById(List<Ride> rides, Long rideId) {
        return rides.stream()
                .filter(r -> r.getId().equals(rideId))
                .findFirst()
                .orElse(null);
    }

    private Driver getDriverFromEntity(String entity, List<Driver> drivers) {
        if (entity == null || !entity.startsWith("DRIVER:")) return null;

        try {
            Long driverId = Long.parseLong(entity.split(":")[1]);
            return drivers.stream()
                    .filter(d -> d.getId().equals(driverId))
                    .findFirst()
                    .orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void assignRideToDriver(Ride ride, Driver pickupDriver, Driver dropoffDriver, String batchId, String method) {
        ride.setPickupDriver(pickupDriver);
        ride.setDropoffDriver(dropoffDriver);
        ride.setDriver(pickupDriver); // Backward compatibility
        updateRideAssignment(ride, batchId, method);
    }

    private void updateRideAssignment(Ride ride, String batchId, String method) {
        ride.setStatus(RideStatus.ASSIGNED);
        ride.setAssignedAt(LocalDateTime.now());
        ride.setAssignedBy("ENHANCED_MEDICAL_OPTIMIZER");
        ride.setOptimizationBatchId(batchId);
        rideRepository.save(ride);
    }

    private Assignment solveWithTimeLimit(RoutingModel routing, int timeoutSeconds) {
        RoutingSearchParameters parameters = main.defaultRoutingSearchParameters()
                .toBuilder()
                .setFirstSolutionStrategy(FirstSolutionStrategy.Value.PATH_CHEAPEST_ARC)
                .setLocalSearchMetaheuristic(LocalSearchMetaheuristic.Value.GUIDED_LOCAL_SEARCH)
                .setTimeLimit(Duration.newBuilder().setSeconds(timeoutSeconds).build())
                .build();

        return routing.solveWithParameters(parameters);
    }

    private String generateBatchId() {
        return "MEDICAL_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) +
                "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    // ============================
    // Fallback Methods
    // ============================

    private OptimizationResult performIntelligentFallback(List<Ride> rides, List<Driver> drivers, String batchId) {
        log.warn("⚠️ Running intelligent medical transport fallback for {} rides", rides.size());

        OptimizationResult result = OptimizationResult.create(batchId, rides.size());

        // Categorize rides for intelligent fallback
        Map<String, List<Ride>> ridesByType = categorizeRidesForFallback(rides);

        // Handle each category in priority order
        for (String category : List.of("emergency", "wheelchair_van", "stretcher_van", "regular")) {
            List<Ride> categoryRides = ridesByType.getOrDefault(category, Collections.emptyList());
            if (!categoryRides.isEmpty()) {
                result.merge(handleRideCategory(categoryRides, drivers, category, batchId));
                drivers = getAvailableDriversAfterAssignment(drivers, result.getAssignedDriverIds());
            }
        }

        return result;
    }

    private Map<String, List<Ride>> categorizeRidesForFallback(List<Ride> rides) {
        Map<String, List<Ride>> categories = new HashMap<>();

        for (Ride ride : rides) {
            String category;
            if (ride.getPriority() == Priority.EMERGENCY) {
                category = "emergency";
            } else {
                String vehicleType = determineRequiredVehicleType(ride);
                category = vehicleType.equals("sedan") || vehicleType.equals("van") ? "regular" : vehicleType;
            }
            categories.computeIfAbsent(category, k -> new ArrayList<>()).add(ride);
        }

        return categories;
    }

    private OptimizationResult handleRideCategory(List<Ride> rides, List<Driver> drivers, String category, String batchId) {
        return switch (category) {
            case "emergency" -> handleEmergencyRidesFallback(rides, drivers, batchId);
            case "wheelchair_van", "stretcher_van" -> handleSpecialVehicleRidesFallback(rides, drivers, category, batchId);
            default -> handleRegularRidesFallback(rides, drivers, batchId);
        };
    }

    private OptimizationResult performSimpleFallback(List<Ride> rides, List<Driver> drivers, String batchId) {
        log.warn("⚠️ Running simple fallback for {} rides with {} drivers", rides.size(), drivers.size());

        OptimizationResult result = OptimizationResult.create(batchId, rides.size());

        int driverIndex = 0;
        for (Ride ride : rides) {
            Driver assignedDriver = findCompatibleDriver(drivers, ride, driverIndex);

            if (assignedDriver != null) {
                if (isRoundTripRide(ride)) {
                    assignRideToDriver(ride, assignedDriver, assignedDriver, batchId, "SIMPLE_FALLBACK");
                } else {
                    Driver dropoffDriver = findCompatibleDriver(drivers, ride, driverIndex + 1);
                    assignRideToDriver(ride, assignedDriver, dropoffDriver != null ? dropoffDriver : assignedDriver, batchId, "SIMPLE_FALLBACK");
                }
                result.addAssignedRide(assignedDriver.getId(), ride.getId());
                driverIndex = (driverIndex + 1) % drivers.size();
            } else {
                result.addUnassignedRide(ride.getId(), "No compatible driver available");
            }
        }

        return result;
    }

    private Driver findCompatibleDriver(List<Driver> drivers, Ride ride, int startIndex) {
        for (int i = 0; i < drivers.size(); i++) {
            Driver candidate = drivers.get((startIndex + i) % drivers.size());
            if (canDriverHandlePatient(candidate, ride.getPatient())) {
                return candidate;
            }
        }
        return null;
    }

    private OptimizationResult handleEmergencyRidesFallback(List<Ride> emergencyRides, List<Driver> drivers, String batchId) {
        OptimizationResult result = new OptimizationResult();

        for (Ride ride : emergencyRides) {
            Driver bestDriver = findBestEmergencyDriver(ride, drivers);
            if (bestDriver != null) {
                assignRideToDriver(ride, bestDriver, bestDriver, batchId, "EMERGENCY_FALLBACK");
                result.addAssignedRide(bestDriver.getId(), ride.getId());
            } else {
                result.addUnassignedRide(ride.getId(), "No emergency-qualified driver available");
            }
        }

        return result;
    }

    private OptimizationResult handleSpecialVehicleRidesFallback(List<Ride> rides, List<Driver> drivers, String vehicleType, String batchId) {
        OptimizationResult result = new OptimizationResult();
        List<Driver> compatibleDrivers = getDriversForVehicleType(drivers, vehicleType);

        if (compatibleDrivers.isEmpty()) {
            rides.forEach(ride -> result.addUnassignedRide(ride.getId(), "No " + vehicleType + " driver available"));
            return result;
        }

        int driverIndex = 0;
        for (Ride ride : rides) {
            Driver assignedDriver = compatibleDrivers.get(driverIndex % compatibleDrivers.size());

            if (isRoundTripRide(ride)) {
                assignRideToDriver(ride, assignedDriver, assignedDriver, batchId, "SPECIAL_VEHICLE_FALLBACK");
            } else {
                Driver dropoffDriver = compatibleDrivers.get((driverIndex + 1) % compatibleDrivers.size());
                assignRideToDriver(ride, assignedDriver, dropoffDriver, batchId, "SPECIAL_VEHICLE_FALLBACK");
            }

            result.addAssignedRide(assignedDriver.getId(), ride.getId());
            driverIndex++;
        }

        return result;
    }

    private OptimizationResult handleRegularRidesFallback(List<Ride> rides, List<Driver> drivers, String batchId) {
        OptimizationResult result = new OptimizationResult();

        if (drivers.isEmpty()) {
            rides.forEach(ride -> result.addUnassignedRide(ride.getId(), "No drivers available"));
            return result;
        }

        int driverIndex = 0;
        for (Ride ride : rides) {
            Driver assignedDriver = drivers.get(driverIndex % drivers.size());

            if (isRoundTripRide(ride)) {
                assignRideToDriver(ride, assignedDriver, assignedDriver, batchId, "REGULAR_FALLBACK");
            } else {
                Driver dropoffDriver = drivers.get((driverIndex + 1) % drivers.size());
                assignRideToDriver(ride, assignedDriver, dropoffDriver, batchId, "REGULAR_FALLBACK");
            }

            result.addAssignedRide(assignedDriver.getId(), ride.getId());
            driverIndex++;
        }

        return result;
    }

    private OptimizationResult createUnassignedResult(List<Ride> rides, String reason) {
        OptimizationResult result = new OptimizationResult();
        rides.forEach(ride -> result.addUnassignedRide(ride.getId(), reason));
        return result;
    }

    // ============================
    // Logging and Audit Methods
    // ============================

    private void logCategorizationResults(RideCategorization categorization) {
        log.info("📊 Ride categorization complete:");
        log.info("   🚨 Emergency: {}", categorization.getEmergencyRides().size());
        log.info("   🔄 Round-trip by vehicle: {}",
                categorization.getRoundTripRidesByVehicleType().entrySet().stream()
                        .map(e -> e.getKey() + "=" + e.getValue().size())
                        .collect(Collectors.joining(", ")));
        log.info("   ➡️ One-way by vehicle: {}",
                categorization.getOneWayRidesByVehicleType().entrySet().stream()
                        .map(e -> e.getKey() + "=" + e.getValue().size())
                        .collect(Collectors.joining(", ")));
    }

    private void logOptimizationResults(String batchId, OptimizationResult result, int totalRides) {
        log.info("✅ Enhanced medical transport optimization complete");
        log.info("📈 Batch: {}, Success Rate: {:.1f}%, Assigned: {}/{}",
                batchId, result.getSuccessRate(), result.getAssignedRideCount(), totalRides);
    }

    private void createDetailedAuditRecord(List<Ride> rides, OptimizationResult result, RideCategorization categorization, String batchId) {
        try {
            List<RideAssignmentAuditDTO> auditDTOs = rides.stream()
                    .map(this::createAuditDTO)
                    .collect(Collectors.toList());

            AssignmentAudit audit = createAuditEntity(rides, result, categorization, batchId, auditDTOs);
            assignmentAuditRepository.save(audit);

            log.info("📊 Enhanced audit record created for batch {}", batchId);
            log.info("📈 Medical transport stats - Wheelchair: {}, Stretcher: {}, Round-trip: {}, Emergency: {}",
                    audit.getWheelchairRides(), audit.getStretcherRides(),
                    audit.getRoundTripRides(), audit.getEmergencyRides());

        } catch (Exception e) {
            log.error("Failed to create enhanced audit record for batch {}: {}", batchId, e.getMessage(), e);
        }
    }

    private AssignmentAudit createAuditEntity(List<Ride> rides, OptimizationResult result,
                                              RideCategorization categorization, String batchId,
                                              List<RideAssignmentAuditDTO> auditDTOs) {
        AssignmentAudit audit = new AssignmentAudit();
        audit.setAssignmentTime(LocalDateTime.now());
        audit.setBatchId(batchId);
        audit.setAssignmentDate(rides.isEmpty() ? LocalDate.now() : rides.get(0).getPickupTime().toLocalDate());
        audit.setTotalRides(rides.size());
        audit.setAssignedRides(result.getAssignedRideCount());
        audit.setUnassignedRides(rides.size() - result.getAssignedRideCount());
        audit.setAssignedDrivers(result.getAssignedDriverCount());
        audit.setSuccessRate(result.getSuccessRate());
        audit.setTriggeredBy("ENHANCED_MEDICAL_OPTIMIZER");
        audit.setOptimizationStrategy("Enhanced OR-Tools VRP with medical transport constraints");

        // Medical transport specific statistics
        audit.setWheelchairRides(categorization.getWheelchairRideCount());
        audit.setStretcherRides(categorization.getStretcherRideCount());
        audit.setRoundTripRides(categorization.getRoundTripRideCount());
        audit.setEmergencyRides(categorization.getEmergencyRides().size());

        audit.setRideAssignmentsSummary(result.getDriverAssignments());
        audit.setRideAssignmentsDetail(auditDTOs);
        audit.setUnassignedReasons(result.getUnassignedReasons());

        return audit;
    }

    private RideAssignmentAuditDTO createAuditDTO(Ride ride) {
        return new RideAssignmentAuditDTO(
                ride.getId(),
                ride.getPatient() != null ? ride.getPatient().getName() : "Unknown",
                ride.getPickupLocation(),
                ride.getDropoffLocation(),
                ride.getPickupDriver() != null ? ride.getPickupDriver().getId() : null,
                ride.getPickupDriver() != null ? ride.getPickupDriver().getName() : "UNASSIGNED",
                ride.getDropoffDriver() != null ? ride.getDropoffDriver().getId() : null,
                ride.getDropoffDriver() != null ? ride.getDropoffDriver().getName() : "UNASSIGNED",
                ride.getRequiredVehicleType(),
                ride.getPriority() != null ? ride.getPriority().name() : "ROUTINE",
                ride.getRideType() != null ? ride.getRideType().name() : "ONE_WAY",
                "ENHANCED_MEDICAL_OPTIMIZER",
                "Medical transport optimization with vehicle matching and flexible driver assignment",
                LocalDateTime.now()
        );
    }

    // ============================
    // Supporting Classes
    // ============================

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

    public static class LocationMapping {
        private List<String> locationStrings = new ArrayList<>();
        private Map<Integer, String> indexToEntity = new HashMap<>();

        public void addLocation(String locationString, String entity) {
            int index = locationStrings.size();
            locationStrings.add(locationString);
            indexToEntity.put(index, entity);
        }

        public int getTotalNodes() {
            return locationStrings.size();
        }

        public String getEntityForIndex(int index) {
            return indexToEntity.get(index);
        }

        public List<String> getLocationStrings() {
            return locationStrings;
        }
    }
}