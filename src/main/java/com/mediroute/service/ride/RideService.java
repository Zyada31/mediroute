package com.mediroute.service.ride;

import com.mediroute.dto.ParseResult;
import com.mediroute.dto.Priority;
import com.mediroute.dto.RideStatus;
import com.mediroute.entity.Patient;
import com.mediroute.entity.Ride;
import com.mediroute.repository.PatientRepository;
import com.mediroute.service.parser.EnhancedExcelRideParser;
import com.mediroute.repository.RideRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RideService {
    Logger log = LoggerFactory.getLogger(RideService.class);
    private final RideRepository rideRepository;
    private final EnhancedExcelRideParser excelRideParser;
    private final EnhancedMedicalTransportOptimizer medicalTransportOptimizer;
    private final PatientRepository patientRepository;

    /**
     * Parse Excel file with medical transport features and optional optimization
     */
    public ParseResult parseExcelWithMedicalFeatures(
            MultipartFile file,
            LocalDate assignmentDate,
            boolean runOptimization) throws IOException {

        log.info("📋 Parsing Excel file with medical transport features for date: {}", assignmentDate);

        ParseResult result = excelRideParser.parseExcelWithMedicalFeatures(
                file, assignmentDate, runOptimization);

        // Validate parsed rides
        validateParsedRides(result.getRides());

        log.info("✅ Parsed {} rides with {} skipped. Success rate: {:.1f}%",
                result.getSuccessfulRows(), result.getSkippedRows(), result.getSuccessRate());

        if (runOptimization && result.isOptimizationRan()) {
            log.info("🎯 Optimization completed as part of parsing process");
        }

        return result;
    }

    /**
     * Parse Excel file using legacy parser (backward compatibility)
     */
    public List<Ride> parseExcelFile(MultipartFile file, LocalDate assignmentDate) throws IOException {
        log.info("📋 Parsing Excel file using legacy parser for date: {}", assignmentDate);

        List<Ride> rides = excelRideParser.parseExcel(file, assignmentDate);
        validateParsedRides(rides);

        log.info("✅ Parsed {} rides using legacy parser", rides.size());
        return rides;
    }

    /**
     * Run medical transport optimization on rides
     */
    public EnhancedMedicalTransportOptimizer.OptimizationResult optimizeSchedule(List<Ride> rides) {
        if (rides == null || rides.isEmpty()) {
            log.warn("⚠️ No rides to optimize");
            return EnhancedMedicalTransportOptimizer.OptimizationResult.empty();
        }

        log.info("🚀 Starting medical transport optimization for {} rides", rides.size());

        EnhancedMedicalTransportOptimizer.OptimizationResult result =
                medicalTransportOptimizer.optimizeSchedule(rides);

        logOptimizationResults(result);
        return result;
    }

    /**
     * Optimize unassigned rides for a specific date
     */
    public EnhancedMedicalTransportOptimizer.OptimizationResult optimizeUnassignedRides(LocalDate date) {
        List<Ride> unassignedRides = rideRepository.findByPickupTimeBetweenAndStatus(
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay(),
                RideStatus.SCHEDULED
        );

        if (unassignedRides.isEmpty()) {
            log.info("✅ No unassigned rides found for date: {}", date);
            return EnhancedMedicalTransportOptimizer.OptimizationResult.empty();
        }

        log.info("🔄 Found {} unassigned rides for date: {}", unassignedRides.size(), date);
        return optimizeSchedule(unassignedRides);
    }

    /**
     * Optimize rides within a time range
     */
    public EnhancedMedicalTransportOptimizer.OptimizationResult optimizeRidesInTimeRange(
            LocalDateTime from, LocalDateTime to) {

        List<Ride> rides = rideRepository.findByPickupTimeBetweenAndStatus(from, to, RideStatus.SCHEDULED);

        log.info("🔄 Found {} rides between {} and {}", rides.size(), from, to);
        return optimizeSchedule(rides);
    }

    /**
     * Get rides by status for a specific date
     */
    public List<Ride> getRidesByStatusAndDate(RideStatus status, LocalDate date) {
        return rideRepository.findByPickupTimeBetweenAndStatus(
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay(),
                status
        );
    }

    /**
     * Get rides requiring specific vehicle types
     */
    public List<Ride> getRidesByVehicleType(String vehicleType, LocalDate date) {
        return rideRepository.findByPickupTimeBetweenAndRequiredVehicleType(
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay(),
                vehicleType
        );
    }

    /**
     * Get emergency rides for immediate assignment
     */
    public List<Ride> getEmergencyRides() {
        return rideRepository.findByPriorityAndStatusOrderByPickupTimeAsc(
                Priority.EMERGENCY,
                RideStatus.SCHEDULED
        );
    }

    /**
     * Re-optimize rides when there are changes (cancellations, new rides)
     */
    public EnhancedMedicalTransportOptimizer.OptimizationResult reOptimizeAffectedRides(
            List<Long> affectedRideIds) {

        if (affectedRideIds == null || affectedRideIds.isEmpty()) {
            return EnhancedMedicalTransportOptimizer.OptimizationResult.empty();
        }

        // Get all rides for the same dates as affected rides
        List<Ride> affectedRides = rideRepository.findAllById(affectedRideIds);
        if (affectedRides.isEmpty()) {
            return EnhancedMedicalTransportOptimizer.OptimizationResult.empty();
        }

        // Find all rides on the same dates that might need re-optimization
        List<LocalDate> affectedDates = affectedRides.stream()
                .map(ride -> ride.getPickupTime().toLocalDate())
                .distinct()
                .collect(Collectors.toList());

        List<Ride> ridesToReOptimize = affectedDates.stream()
                .flatMap(date -> getRidesByStatusAndDate(RideStatus.SCHEDULED, date).stream())
                .collect(Collectors.toList());

        log.info("🔄 Re-optimizing {} rides affected by changes to {} rides on {} dates",
                ridesToReOptimize.size(), affectedRideIds.size(), affectedDates.size());

        return optimizeSchedule(ridesToReOptimize);
    }

    /**
     * Validate parsed rides for required fields and data integrity
     */
    private void validateParsedRides(List<Ride> rides) {
        if (rides == null || rides.isEmpty()) {
            return;
        }

        int invalidRides = 0;
        for (Ride ride : rides) {
            if (!isValidRide(ride)) {
                invalidRides++;
                log.warn("⚠️ Invalid ride found: ID={}, missing required fields", ride.getId());
            }
        }

        if (invalidRides > 0) {
            log.warn("⚠️ Found {} rides with missing required fields out of {} total rides",
                    invalidRides, rides.size());
        }
    }

    /**
     * Check if a ride has all required fields for optimization
     */
    private boolean isValidRide(Ride ride) {
        return ride != null &&
                ride.getPickupLocation() != null && !ride.getPickupLocation().isBlank() &&
                ride.getDropoffLocation() != null && !ride.getDropoffLocation().isBlank() &&
                ride.getPickupLat() != null && ride.getPickupLng() != null &&
                ride.getDropoffLat() != null && ride.getDropoffLng() != null &&
                ride.getPickupTime() != null &&
                ride.getPatient() != null;
    }

    /**
     * Log optimization results
     */
    private void logOptimizationResults(EnhancedMedicalTransportOptimizer.OptimizationResult result) {
        if (result == null) {
            log.warn("⚠️ Optimization result is null");
            return;
        }

        log.info("📊 Optimization Results:");
        log.info("   📈 Success Rate: {:.1f}%", result.getSuccessRate());
        log.info("   ✅ Assigned Rides: {}", result.getAssignedRideCount());
        log.info("   👥 Drivers Used: {}", result.getAssignedDriverCount());

        if (!result.getUnassignedReasons().isEmpty()) {
            log.warn("   ❌ Unassigned Rides: {}", result.getUnassignedReasons().size());
            result.getUnassignedReasons().entrySet().stream()
                    .limit(5) // Show first 5 unassigned reasons
                    .forEach(entry -> log.warn("      Ride {}: {}", entry.getKey(), entry.getValue()));
        }

        if (result.getBatchId() != null) {
            log.info("   🆔 Batch ID: {}", result.getBatchId());
        }
    }

    /**
     * Get optimization statistics for reporting
     */
    public OptimizationStats getOptimizationStats(LocalDate date) {
        List<Ride> allRides = rideRepository.findByPickupTimeBetween(
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay()
        );

        long assignedRides = allRides.stream()
                .filter(ride -> ride.getPickupDriver() != null || ride.getDriver() != null)
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

        return new OptimizationStats(
                allRides.size(),
                (int) assignedRides,
                allRides.size() - (int) assignedRides,
                (int) emergencyRides,
                (int) wheelchairRides,
                (int) roundTripRides,
                allRides.isEmpty() ? 0.0 : (assignedRides * 100.0) / allRides.size()
        );
    }

    /**
     * Statistics class for optimization reporting
     */
    public static class OptimizationStats {
        private final int totalRides;
        private final int assignedRides;
        private final int unassignedRides;
        private final int emergencyRides;
        private final int wheelchairRides;
        private final int roundTripRides;
        private final double successRate;

        public OptimizationStats(int totalRides, int assignedRides, int unassignedRides,
                                 int emergencyRides, int wheelchairRides, int roundTripRides,
                                 double successRate) {
            this.totalRides = totalRides;
            this.assignedRides = assignedRides;
            this.unassignedRides = unassignedRides;
            this.emergencyRides = emergencyRides;
            this.wheelchairRides = wheelchairRides;
            this.roundTripRides = roundTripRides;
            this.successRate = successRate;
        }

        // Getters
        public int getTotalRides() { return totalRides; }
        public int getAssignedRides() { return assignedRides; }
        public int getUnassignedRides() { return unassignedRides; }
        public int getEmergencyRides() { return emergencyRides; }
        public int getWheelchairRides() { return wheelchairRides; }
        public int getRoundTripRides() { return roundTripRides; }
        public double getSuccessRate() { return successRate; }
    }
    /**
     * Find a patient by their phone number
     */
    @Transactional
    public Optional<Patient> findByPhoneNumber(String phoneNumber) {
        return patientRepository.findByPhoneNumber(phoneNumber);
    }
}