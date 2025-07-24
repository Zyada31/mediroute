package com.mediroute.DTO;

import java.time.LocalDateTime;
import java.util.List;

public record DriverRideSummary(
        Long driverId,
        String driverName,
        int totalAssignedRides,
        List<LocalDateTime> assignedRideTimes
) {}
