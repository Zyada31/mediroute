package com.mediroute.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    @Schema(description = "Driver assignments map (driver ID -> list of ride IDs)")
    @Builder.Default
    private Map<Long, List<Long>> driverAssignments = new HashMap<>();

    @Schema(description = "Unassigned rides with reasons")
    @Builder.Default
    private Map<Long, String> unassignedReasons = new HashMap<>();

    @Schema(description = "Optimization timestamp")
    private LocalDateTime optimizationTime;

    @Schema(description = "Success rate percentage")
    private Double successRate;

    @Schema(description = "Whether optimization ran successfully")
    @Builder.Default
    private Boolean optimizationRan = false;

    @Schema(description = "Optimization error message if any")
    private String optimizationError;

    @Schema(description = "Number of assigned rides")
    private Integer assignedRideCount;

    @Schema(description = "Number of drivers used")
    private Integer assignedDriverCount;

    public static OptimizationResult empty() {
        return OptimizationResult.builder()
                .totalRides(0)
                .successRate(0.0)
                .optimizationTime(LocalDateTime.now())
                .assignedRideCount(0)
                .assignedDriverCount(0)
                .build();
    }

    public void addAssignedRide(Long driverId, Long rideId) {
        driverAssignments.computeIfAbsent(driverId, k -> new java.util.ArrayList<>()).add(rideId);
        updateCounts();
    }

    public void addUnassignedRide(Long rideId, String reason) {
        unassignedReasons.put(rideId, reason);
    }

    public int getAssignedRideCount() {
        if (assignedRideCount != null) return assignedRideCount;
        return driverAssignments.values().stream().mapToInt(List::size).sum();
    }

    public int getAssignedDriverCount() {
        if (assignedDriverCount != null) return assignedDriverCount;
        return driverAssignments.size();
    }

    public Set<Long> getAssignedDriverIds() {
        return driverAssignments.keySet();
    }

    public void calculateSuccessRate() {
        if (totalRides != null && totalRides > 0) {
            this.successRate = (getAssignedRideCount() * 100.0) / totalRides;
        } else {
            this.successRate = 0.0;
        }
    }

    private void updateCounts() {
        this.assignedRideCount = getAssignedRideCount();
        this.assignedDriverCount = getAssignedDriverCount();
        calculateSuccessRate();
    }
}
