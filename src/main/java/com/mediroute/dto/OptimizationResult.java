package com.mediroute.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.*;

@Data
public class OptimizationResult {
    private String batchId;
    private int totalRides;
    private Map<Long, List<Long>> driverAssignments = new HashMap<>();
    private Map<Long, String> unassignedRides = new HashMap<>();
    private LocalDateTime optimizationTime = LocalDateTime.now();

    public void addAssignedRide(Long driverId, Long rideId) {
        driverAssignments.computeIfAbsent(driverId, k -> new ArrayList<>()).add(rideId);
    }

    public void addUnassignedRide(Long rideId, String reason) {
        unassignedRides.put(rideId, reason);
    }

    public void merge(OptimizationResult other) {
        if (other != null) {
            for (Map.Entry<Long, List<Long>> entry : other.driverAssignments.entrySet()) {
                this.driverAssignments.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).addAll(entry.getValue());
            }
            this.unassignedRides.putAll(other.unassignedRides);
        }
    }

    public int getAssignedRideCount() {
        return driverAssignments.values().stream().mapToInt(List::size).sum();
    }

    public int getUnassignedRideCount() {
        return unassignedRides.size();
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

    public Map<Long, List<Long>> getDriverAssignments() {
        return driverAssignments;
    }

    public static OptimizationResult empty() {
        return new OptimizationResult();
    }

    public void setTotalRides(int totalRides) {
        this.totalRides = totalRides;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }
}