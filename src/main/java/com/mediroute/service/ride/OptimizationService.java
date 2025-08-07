package com.mediroute.service.ride;

import com.mediroute.dto.OptimizationResult;
import com.mediroute.entity.Ride;
import com.mediroute.service.ride.EnhancedMedicalTransportOptimizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OptimizationService {

    private final EnhancedMedicalTransportOptimizer enhancedOptimizer;

    /**
     * Main optimization entry point - wraps your existing implementation
     */
    public OptimizationResult optimizeSchedule(List<Ride> rides) {
        log.info("🚀 Starting optimization for {} rides", rides.size());

        var result = enhancedOptimizer.optimizeSchedule(rides);

        // Convert to our DTO format
        return OptimizationResult.builder()
                .batchId(result.getBatchId())
                .totalRides(result.getTotalRides())
                .driverAssignments(result.getDriverAssignments())
                .unassignedReasons(result.getUnassignedReasons())
                .optimizationTime(result.getOptimizationTime())
                .successRate(result.getSuccessRate())
                .optimizationRan(true)
                .build();
    }

    /**
     * Optimize unassigned rides in time range
     */
    public OptimizationResult optimizeUnassignedRides(LocalDateTime start, LocalDateTime end) {
        log.info("🔄 Optimizing unassigned rides between {} and {}", start, end);

        // Your existing implementation would go here
        // For now, delegate to the main optimizer
        return OptimizationResult.empty();
    }
    /**
     * Re-optimize affected rides
     */
    public OptimizationResult reOptimizeAffectedRides(List<Long> affectedRideIds) {
        log.info("🔄 Re-optimizing {} affected rides", affectedRideIds.size());

        // Your existing implementation would go here
        return OptimizationResult.empty();
    }
}