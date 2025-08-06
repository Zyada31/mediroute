package com.mediroute.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AssignmentAuditDTO(
        Long id,
        LocalDateTime assignmentTime,
        LocalDate assignmentDate,
        int totalRides,
        int assignedDrivers,
        int unassignedRides,
        String triggeredBy,
        List<RideAssignmentAuditDTO> rideAssignments
) {}