package com.mediroute.service.ride;

import com.mediroute.dto.OptimizationResult;
import com.mediroute.entity.Ride;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Integration service that properly manages transactions and entity initialization
 * for the optimization process
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OptimizationIntegrationService {

    private final RideService rideService;
    private final EnhancedMedicalTransportOptimizer enhancedOptimizer;

    /**
     * Main optimization entry point that properly handles entity initialization
     */
    @Transactional
    public OptimizationResult optimizeSchedule(List<Ride> rides) {
        log.info("🚀 Starting optimization integration for {} rides", rides.size());

        try {
            // If rides don't have IDs (new rides), use them directly
            // If they have IDs, reload them with proper initialization
            List<Ride> preparedRides = prepareRidesForOptimization(rides);

            // Run optimization with properly initialized entities
            var optimizerResult = enhancedOptimizer.optimizeSchedule(preparedRides);

            // Convert to our DTO format
            return OptimizationResult.builder()
                    .batchId(optimizerResult.getBatchId())
                    .totalRides(optimizerResult.getTotalRides())
                    .assignedRideCount(optimizerResult.getAssignedRideCount())
                    .unassignedRideCount(optimizerResult.getTotalRides() - optimizerResult.getAssignedRideCount())
                    .assignedDriverCount(optimizerResult.getAssignedDriverCount())
                    .driverAssignments(optimizerResult.getDriverAssignments())
                    .unassignedReasons(optimizerResult.getUnassignedReasons())
                    .optimizationTime(optimizerResult.getOptimizationTime())
                    .successRate(optimizerResult.getSuccessRate())
                    .optimizationRan(true)
                    .build();

        } catch (Exception e) {
            log.error("❌ Optimization integration failed: {}", e.getMessage(), e);
            return OptimizationResult.builder()
                    .totalRides(rides.size())
                    .assignedRideCount(0)
                    .unassignedRideCount(rides.size())
                    .successRate(0.0)
                    .optimizationRan(false)
                    .optimizationError(e.getMessage())
                    .build();
        }
    }

    /**
     * Optimize rides for a specific date
     */
    @Transactional
    public OptimizationResult optimizeRidesForDate(LocalDate date) {
        log.info("🔄 Optimizing rides for date: {}", date);

        try {
            // Get unassigned rides for the date with proper entity initialization
            List<Ride> unassignedRides = rideService.findUnassignedRides(date);

            if (unassignedRides.isEmpty()) {
                log.info("No unassigned rides found for date: {}", date);
                return OptimizationResult.empty();
            }

            return optimizeSchedule(unassignedRides);

        } catch (Exception e) {
            log.error("❌ Date-based optimization failed for {}: {}", date, e.getMessage(), e);
            return OptimizationResult.builder()
                    .optimizationRan(false)
                    .optimizationError(e.getMessage())
                    .build();
        }
    }

    /**
     * Optimize specific rides by IDs
     */
    @Transactional
    public OptimizationResult optimizeSpecificRides(List<Long> rideIds) {
        log.info("🔄 Optimizing {} specific rides", rideIds.size());

        try {
            // Prepare rides with proper entity initialization
            List<Ride> rides = rideService.prepareRidesForOptimization(rideIds);

            if (rides.isEmpty()) {
                log.warn("No valid rides found for optimization from IDs: {}", rideIds);
                return OptimizationResult.empty();
            }

            return optimizeSchedule(rides);

        } catch (Exception e) {
            log.error("❌ Specific ride optimization failed: {}", e.getMessage(), e);
            return OptimizationResult.builder()
                    .optimizationRan(false)
                    .optimizationError(e.getMessage())
                    .build();
        }
    }

    /**
     * Optimize rides in a time range
     */
    @Transactional
    public OptimizationResult optimizeRidesInTimeRange(LocalDateTime start, LocalDateTime end) {
        log.info("🔄 Optimizing rides between {} and {}", start, end);

        try {
            // Find rides for optimization with proper entity initialization
            LocalDate date = start.toLocalDate();
            List<Ride> rides = rideService.findRidesForOptimization(date);

            // Filter by time range
            List<Ride> ridesInRange = rides.stream()
                    .filter(ride -> !ride.getPickupTime().isBefore(start) &&
                            !ride.getPickupTime().isAfter(end))
                    .filter(ride -> ride.getPickupDriver() == null &&
                            ride.getDropoffDriver() == null) // Only unassigned
                    .toList();

            if (ridesInRange.isEmpty()) {
                log.info("No unassigned rides found in time range {} to {}", start, end);
                return OptimizationResult.empty();
            }

            return optimizeSchedule(ridesInRange);

        } catch (Exception e) {
            log.error("❌ Time range optimization failed: {}", e.getMessage(), e);
            return OptimizationResult.builder()
                    .optimizationRan(false)
                    .optimizationError(e.getMessage())
                    .build();
        }
    }

    /**
     * Prepare rides for optimization with proper entity initialization
     */
    @Transactional(readOnly = true)
    public List<Ride> prepareRidesForOptimization(List<Ride> rides) {
        if (rides == null || rides.isEmpty()) {
            return List.of();
        }

        // Check if rides have IDs (existing rides) or are new
        List<Long> rideIds = rides.stream()
                .map(Ride::getId)
                .filter(id -> id != null)
                .toList();

        if (rideIds.isEmpty()) {
            // These are new rides without IDs, ensure patients are properly initialized
            log.debug("Preparing {} new rides for optimization", rides.size());
            return rides.stream()
                    .peek(this::initializeNewRideEntities)
                    .toList();
        } else if (rideIds.size() == rides.size()) {
            // All rides have IDs, reload with proper initialization
            log.debug("Reloading {} existing rides for optimization", rides.size());
            return rideService.prepareRidesForOptimization(rideIds);
        } else {
            // Mixed case - some new, some existing
            log.debug("Preparing mixed rides for optimization: {} new, {} existing",
                    rides.size() - rideIds.size(), rideIds.size());

            List<Ride> preparedRides = rideService.prepareRidesForOptimization(rideIds);

            // Add new rides
            rides.stream()
                    .filter(ride -> ride.getId() == null)
                    .peek(this::initializeNewRideEntities)
                    .forEach(preparedRides::add);

            return preparedRides;
        }
    }

    /**
     * Initialize entities for new rides that don't have IDs yet
     */
    private void initializeNewRideEntities(Ride ride) {
        try {
            // For new rides, just ensure basic properties are accessible
            if (ride.getPatient() != null) {
                // Access key properties to ensure they're loaded
                ride.getPatient().getName();
                ride.getPatient().getRequiresWheelchair();
                ride.getPatient().getRequiresStretcher();
                ride.getPatient().getRequiresOxygen();
                ride.getPatient().getMobilityLevel();
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
            log.warn("Failed to initialize new ride entities: {}", e.getMessage());
        }
    }

    /**
     * Get optimization statistics for monitoring
     */
    @Transactional(readOnly = true)
    public OptimizationStats getOptimizationStats(LocalDate date) {
        try {
            List<Ride> allRides = rideService.findRidesByDate(date).stream()
                    .map(dto -> {
                        // Create minimal ride objects for stats
                        Ride ride = new Ride();
                        ride.setId(dto.getId());
                        ride.setStatus(dto.getStatus());
                        ride.setPickupTime(dto.getPickupTime());
                        ride.setPriority(dto.getPriority());
                        ride.setRequiredVehicleType(dto.getRequiredVehicleType());
                        ride.setIsRoundTrip(dto.getIsRoundTrip());
                        return ride;
                    })
                    .toList();

            long totalRides = allRides.size();
            long assignedRides = allRides.stream()
                    .filter(ride -> ride.getStatus() == com.mediroute.dto.RideStatus.ASSIGNED ||
                            ride.getStatus() == com.mediroute.dto.RideStatus.COMPLETED)
                    .count();
            long unassignedRides = totalRides - assignedRides;

            long emergencyRides = allRides.stream()
                    .filter(ride -> ride.getPriority() == com.mediroute.dto.Priority.EMERGENCY)
                    .count();

            long wheelchairRides = allRides.stream()
                    .filter(ride -> "wheelchair_van".equals(ride.getRequiredVehicleType()))
                    .count();

            long roundTripRides = allRides.stream()
                    .filter(ride -> Boolean.TRUE.equals(ride.getIsRoundTrip()))
                    .count();

            double assignmentRate = totalRides > 0 ? (assignedRides * 100.0 / totalRides) : 0.0;

            return OptimizationStats.builder()
                    .date(date)
                    .totalRides((int) totalRides)
                    .assignedRides((int) assignedRides)
                    .unassignedRides((int) unassignedRides)
                    .emergencyRides((int) emergencyRides)
                    .wheelchairRides((int) wheelchairRides)
                    .roundTripRides((int) roundTripRides)
                    .assignmentRate(assignmentRate)
                    .build();

        } catch (Exception e) {
            log.error("Error getting optimization stats for {}: {}", date, e.getMessage(), e);
            return OptimizationStats.builder()
                    .date(date)
                    .totalRides(0)
                    .assignedRides(0)
                    .unassignedRides(0)
                    .assignmentRate(0.0)
                    .build();
        }
    }

    /**
     * Statistics class for optimization monitoring
     */
    public static class OptimizationStats {
        private LocalDate date;
        private int totalRides;
        private int assignedRides;
        private int unassignedRides;
        private int emergencyRides;
        private int wheelchairRides;
        private int roundTripRides;
        private double assignmentRate;

        public static OptimizationStatsBuilder builder() {
            return new OptimizationStatsBuilder();
        }

        // Getters
        public LocalDate getDate() { return date; }
        public int getTotalRides() { return totalRides; }
        public int getAssignedRides() { return assignedRides; }
        public int getUnassignedRides() { return unassignedRides; }
        public int getEmergencyRides() { return emergencyRides; }
        public int getWheelchairRides() { return wheelchairRides; }
        public int getRoundTripRides() { return roundTripRides; }
        public double getAssignmentRate() { return assignmentRate; }

        // Builder class
        public static class OptimizationStatsBuilder {
            private OptimizationStats stats = new OptimizationStats();

            public OptimizationStatsBuilder date(LocalDate date) {
                stats.date = date;
                return this;
            }

            public OptimizationStatsBuilder totalRides(int totalRides) {
                stats.totalRides = totalRides;
                return this;
            }

            public OptimizationStatsBuilder assignedRides(int assignedRides) {
                stats.assignedRides = assignedRides;
                return this;
            }

            public OptimizationStatsBuilder unassignedRides(int unassignedRides) {
                stats.unassignedRides = unassignedRides;
                return this;
            }

            public OptimizationStatsBuilder emergencyRides(int emergencyRides) {
                stats.emergencyRides = emergencyRides;
                return this;
            }

            public OptimizationStatsBuilder wheelchairRides(int wheelchairRides) {
                stats.wheelchairRides = wheelchairRides;
                return this;
            }

            public OptimizationStatsBuilder roundTripRides(int roundTripRides) {
                stats.roundTripRides = roundTripRides;
                return this;
            }

            public OptimizationStatsBuilder assignmentRate(double assignmentRate) {
                stats.assignmentRate = assignmentRate;
                return this;
            }

            public OptimizationStats build() {
                return stats;
            }
        }
    }
}