package com.mediroute.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class RideAssignmentAuditDTO {
    private Long rideId;
    private String patientName;
    private String pickupLocation;
    private String dropoffLocation;
    private Long pickupDriverId;
    private String pickupDriverName;
    private Long dropoffDriverId;
    private String dropoffDriverName;
    private VehicleTypeEnum requiredVehicleType;
    private String priority;
    private String rideType;
    private final String enhancedMedicalOptimizer;
    private String assignedBy;
    private String assignmentMethod;
    private LocalDateTime assignmentTime;
    private String notes;

    public RideAssignmentAuditDTO(Long id, String patientName, String pickupLocation, String dropoffLocation, Long pickupDriverId, 
                                  String pickupDriverName, Long dropoffDriverId, String dropoffDriverName, 
                                  String requiredVehicleType, String priority, String rideType, String enhancedMedicalOptimizer,
                                  String assignmentMethod, LocalDateTime now) {
        this.patientName = patientName;
        this.pickupLocation = pickupLocation;
        this.dropoffLocation = dropoffLocation;
        this.pickupDriverId = pickupDriverId;
        this.dropoffDriverName = dropoffDriverName;
        this.priority = priority;
        this.rideType = rideType;
        this.enhancedMedicalOptimizer = enhancedMedicalOptimizer;
    }
}