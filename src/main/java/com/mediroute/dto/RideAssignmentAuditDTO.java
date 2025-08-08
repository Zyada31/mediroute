package com.mediroute.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RideAssignmentAuditDTO {
    private Long rideId;
    private String patientName;
    private String pickupLocation;
    private String dropoffLocation;
    private Long pickupDriverId;
    private String pickupDriverName;
    private Long dropoffDriverId;
    private String dropoffDriverName;
    private String requiredVehicleType;
    private String priority;
    private String rideType;
    private String assignedBy;
    private String assignmentMethod;
    private LocalDateTime assignmentTime;
    private String notes;

    // Additional constructor for backward compatibility
    public RideAssignmentAuditDTO(Long rideId, String patientName, String pickupLocation,
                                  String dropoffLocation, Long pickupDriverId, String pickupDriverName,
                                  Long dropoffDriverId, String dropoffDriverName, String requiredVehicleType,
                                  String priority, String rideType, String assignedBy, String assignmentMethod,
                                  LocalDateTime assignmentTime) {
        this.rideId = rideId;
        this.patientName = patientName;
        this.pickupLocation = pickupLocation;
        this.dropoffLocation = dropoffLocation;
        this.pickupDriverId = pickupDriverId;
        this.pickupDriverName = pickupDriverName;
        this.dropoffDriverId = dropoffDriverId;
        this.dropoffDriverName = dropoffDriverName;
        this.requiredVehicleType = requiredVehicleType;
        this.priority = priority;
        this.rideType = rideType;
        this.assignedBy = assignedBy;
        this.assignmentMethod = assignmentMethod;
        this.assignmentTime = assignmentTime;
    }
}
