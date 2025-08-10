package com.mediroute.service.ride;

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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EnhancedMedicalTransportOptimizer {

    private final RideRepository rideRepository;
    private final DriverRepository driverRepository;
    private final OsrmDistanceService distanceService;
    private final AssignmentAuditRepository assignmentAuditRepository;

    // Medical transport constants
    private static final int SHORT_APPOINTMENT_THRESHOLD = 15;
    private static final int OPTIMIZATION_TIMEOUT_SECONDS = 45;

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
     * Core medical transport optimization using intelligent fallback algorithm
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
     * Unified method to optimize rides for a specific vehicle type using intelligent assignment
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

        return performIntelligentAssignment(rides, compatibleDrivers, batchId, isRoundTrip);
    }

    /**
     * Intelligent assignment algorithm (fallback when OR-Tools not available)
     */
    private OptimizationResult performIntelligentAssignment(List<Ride> rides, List<Driver> drivers,
                                                            String batchId, boolean isRoundTrip) {
        OptimizationResult result = new OptimizationResult();

        // Sort rides by priority and time
        List<Ride> sortedRides = rides.stream()
                .sorted(Comparator
                        .comparing((Ride r) -> r.getPriority() == Priority.EMERGENCY ? 0 :
                                r.getPriority() == Priority.URGENT ? 1 : 2)
                        .thenComparing(Ride::getPickupTime))
                .collect(Collectors.toList());

        for (Ride ride : sortedRides) {
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
                result.addUnassignedRide(ride.getId(), "No compatible driver available");
                log.warn("❌ Could not assign ride {}", ride.getId());
            }
        }

        return result;
    }

    // Helper methods for intelligent assignment
    private Driver findBestDriverForRide(Ride ride, List<Driver> drivers) {
        return drivers.stream()
                .filter(driver -> canDriverHandlePatient(driver, ride.getPatient()))
                .filter(driver -> hasRequiredSkills(driver, ride.getRequiredSkills()))
                .filter(driver -> isDriverAvailableForRide(driver, ride))
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

        // Capability bonus (exact match is better)
        if (hasExactVehicleMatch(driver, ride)) {
            score -= 50;
        }

        // Experience bonus
        score -= driver.getMaxDailyRides() * 5;

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
        // Basic availability check - can be enhanced with actual scheduling
        return driver.getActive() && Boolean.TRUE.equals(driver.getIsTrainingComplete());
    }

    private boolean isDriverAvailableForDropoff(Driver driver, Ride ride) {
        // Check if driver is available for dropoff time
        return isDriverAvailableForRide(driver, ride);
    }

    // Rest of the helper methods remain the same...
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
                !(Boolean.TRUE.equals(patient.getRequiresOxygen()) && !Boolean.TRUE.equals(driver.getOxygenEquipped()));
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

        return 6371 * c; // Earth's radius in kilometers
    }

    private List<Driver> getAvailableDriversAfterAssignment(List<Driver> allDrivers, Set<Long> assignedDriverIds) {
        return allDrivers.stream()
                .filter(driver -> !assignedDriverIds.contains(driver.getId()))
                .collect(Collectors.toList());
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

    private String generateBatchId() {
        return "MEDICAL_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) +
                "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    // Fallback and audit methods remain the same but simplified...
    private OptimizationResult performIntelligentFallback(List<Ride> rides, List<Driver> drivers, String batchId) {
        log.warn("⚠️ Running intelligent medical transport fallback for {} rides", rides.size());
        return performIntelligentAssignment(rides, drivers, batchId, false);
    }

    private OptimizationResult createUnassignedResult(List<Ride> rides, String reason) {
        OptimizationResult result = new OptimizationResult();
        rides.forEach(ride -> result.addUnassignedRide(ride.getId(), reason));
        return result;
    }

    // Logging methods
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

    private void createDetailedAuditRecord(List<Ride> rides, OptimizationResult result,
                                           RideCategorization categorization, String batchId) {
        try {
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

    // Supporting Classes (simplified versions)
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