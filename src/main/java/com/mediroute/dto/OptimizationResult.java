package com.mediroute.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//@Schema(description = "Optimization result")
//public class OptimizationResult {
//    @Schema(description = "Optimization batch ID")
//    private String batchId;
//
//    @Schema(description = "Total number of rides")
//    private Integer totalRides;
//
//    @Schema(description = "Driver assignments map (driver ID -> list of ride IDs)")
//    @Builder.Default
//    private Map<Long, List<Long>> driverAssignments = new HashMap<>();
//
//    @Schema(description = "Unassigned rides with reasons")
//    @Builder.Default
//    private Map<Long, String> unassignedReasons = new HashMap<>();
//
//    @Schema(description = "Optimization timestamp")
//    private LocalDateTime optimizationTime;
//
//    @Schema(description = "Success rate percentage")
//    private Double successRate;
//
//    @Schema(description = "Whether optimization ran successfully")
//    @Builder.Default
//    private Boolean optimizationRan = false;
//
//    @Schema(description = "Optimization error message if any")
//    private String optimizationError;
//
//    @Schema(description = "Number of assigned rides")
//    private Integer assignedRideCount;
//
//    @Schema(description = "Number of drivers used")
//    private Integer assignedDriverCount;
//
//    public static OptimizationResult empty() {
//        return OptimizationResult.builder()
//                .totalRides(0)
//                .successRate(0.0)
//                .optimizationTime(LocalDateTime.now())
//                .assignedRideCount(0)
//                .assignedDriverCount(0)
//                .build();
//    }
//
//    public void addAssignedRide(Long driverId, Long rideId) {
//        driverAssignments.computeIfAbsent(driverId, k -> new java.util.ArrayList<>()).add(rideId);
//        updateCounts();
//    }
//
//    public void addUnassignedRide(Long rideId, String reason) {
//        unassignedReasons.put(rideId, reason);
//    }
//
//    public int getAssignedRideCount() {
//        if (assignedRideCount != null) return assignedRideCount;
//        return driverAssignments.values().stream().mapToInt(List::size).sum();
//    }
//
//    public int getAssignedDriverCount() {
//        if (assignedDriverCount != null) return assignedDriverCount;
//        return driverAssignments.size();
//    }
//
//    public Set<Long> getAssignedDriverIds() {
//        return driverAssignments.keySet();
//    }
//
//    public void calculateSuccessRate() {
//        if (totalRides != null && totalRides > 0) {
//            this.successRate = (getAssignedRideCount() * 100.0) / totalRides;
//        } else {
//            this.successRate = 0.0;
//        }
//    }
//
//    private void updateCounts() {
//        this.assignedRideCount = getAssignedRideCount();
//        this.assignedDriverCount = getAssignedDriverCount();
//        calculateSuccessRate();
//    }
//}
/**
 * Single Optimization Result DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Optimization result")
public class OptimizationResult {
    @Schema(description = "Optimization batch ID")
    private String batchId;

    @Schema(description = "Total number of rides")
    private Integer totalRides;

    @Schema(description = "Number of assigned rides")
    private Integer assignedRideCount;

    @Schema(description = "Number of unassigned rides")
    private Integer unassignedRideCount;

    @Schema(description = "Number of drivers used")
    private Integer assignedDriverCount;

    @Schema(description = "Success rate percentage")
    private Double successRate;

    @Schema(description = "Driver assignments map (driver ID -> list of ride IDs)")
    @Builder.Default
    private Map<Long, List<Long>> driverAssignments = new HashMap<>();

    @Schema(description = "Unassigned rides with reasons")
    @Builder.Default
    private Map<Long, String> unassignedReasons = new HashMap<>();

    @Schema(description = "Optimization timestamp")
    private LocalDateTime optimizationTime;

    @Schema(description = "Optimization duration in seconds")
    private Long optimizationDurationSeconds;

    @Schema(description = "Whether optimization ran successfully")
    @Builder.Default
    private Boolean optimizationRan = false;

    @Schema(description = "Optimization error message if any")
    private String optimizationError;

    @Schema(description = "Optimization strategy used")
    private String optimizationStrategy;

    // Medical transport specific metrics
    @Schema(description = "Number of wheelchair rides assigned")
    private Integer wheelchairRidesAssigned;

    @Schema(description = "Number of stretcher rides assigned")
    private Integer stretcherRidesAssigned;

    @Schema(description = "Number of emergency rides assigned")
    private Integer emergencyRidesAssigned;

    public static OptimizationResult empty() {
        return OptimizationResult.builder()
                .batchId(generateBatchId())
                .totalRides(0)
                .assignedRideCount(0)
                .unassignedRideCount(0)
                .assignedDriverCount(0)
                .successRate(0.0)
                .optimizationTime(LocalDateTime.now())
                .optimizationRan(false)
                .build();
    }

    private static String generateBatchId() {
        return "OPT_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }

    public void calculateMetrics() {
        // Calculate counts
        this.assignedRideCount = driverAssignments.values().stream()
                .mapToInt(List::size).sum();
        this.unassignedRideCount = unassignedReasons.size();
        this.assignedDriverCount = driverAssignments.size();

        // Calculate success rate
        if (totalRides != null && totalRides > 0) {
            this.successRate = (assignedRideCount * 100.0) / totalRides;
        } else {
            this.successRate = 0.0;
        }
    }

    public void addAssignedRide(Long driverId, Long rideId) {
        driverAssignments.computeIfAbsent(driverId, k -> new ArrayList<>()).add(rideId);
        calculateMetrics();
    }

    public void addUnassignedRide(Long rideId, String reason) {
        unassignedReasons.put(rideId, reason);
        calculateMetrics();
    }
}
