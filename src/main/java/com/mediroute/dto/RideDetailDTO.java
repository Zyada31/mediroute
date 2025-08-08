package com.mediroute.dto;

import com.mediroute.entity.Ride;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RideDetailDTO {
    private Long id;
    private String patientName;
    private String patientPhone;
    private String pickupLocation;
    private String dropoffLocation;
    private LocalDateTime pickupTime;
    private LocalDateTime dropoffTime;
    private RideStatus status;
    private Priority priority;
    private String pickupDriverName;
    private String dropoffDriverName;
    private String requiredVehicleType;
    private Double distance;
    private Integer estimatedDuration;
    private Boolean isRoundTrip;

    // Static factory method to create from entity
    public static RideDetailDTO fromEntity(Ride ride) {
        return RideDetailDTO.builder()
                .id(ride.getId())
                .patientName(ride.getPatient() != null ? ride.getPatient().getName() : null)
                .patientPhone(ride.getPatient() != null ? ride.getPatient().getPhone() : null)
                .pickupLocation(ride.getPickupLocation() != null ? ride.getPickupLocation().getAddress() : null)
                .dropoffLocation(ride.getDropoffLocation() != null ? ride.getDropoffLocation().getAddress() : null)
                .pickupTime(ride.getPickupTime())
                .dropoffTime(ride.getDropoffTime())
                .status(ride.getStatus())
                .priority(ride.getPriority())
                .pickupDriverName(ride.getPickupDriver() != null ? ride.getPickupDriver().getName() : null)
                .dropoffDriverName(ride.getDropoffDriver() != null ? ride.getDropoffDriver().getName() : null)
                .requiredVehicleType(ride.getRequiredVehicleType())
                .distance(ride.getDistance())
                .estimatedDuration(ride.getEstimatedDuration())
                .isRoundTrip(ride.getIsRoundTrip())
                .build();
    }
}