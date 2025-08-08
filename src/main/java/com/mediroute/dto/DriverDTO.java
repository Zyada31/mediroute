package com.mediroute.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class DriverDTO {
    private String name;
    private String email;
    private String phone;
    private String vehicleType;
    private Map<String, Boolean> skills;
    private LocalDateTime shiftStart;
    private LocalDateTime shiftEnd;
    private String baseLocation; // class with lat/lng
    private Integer maxDailyRides;
    private Boolean wheelchairAccessible;
    private Boolean stretcherCapable;
    private Boolean oxygenEquipped;
    private Boolean trainingComplete;
    private Boolean isTrainingComplete;
    private List<String> certifications;

    // Getters & Setters
}
