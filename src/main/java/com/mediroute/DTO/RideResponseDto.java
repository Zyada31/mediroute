package com.mediroute.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class RideResponseDto {
    private Long id;
    private String patientName;
    private String pickupLocation;
    private LocalDateTime pickupTime;
    // etc.
}