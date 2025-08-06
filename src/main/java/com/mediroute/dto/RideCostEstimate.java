package com.mediroute.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RideCostEstimate {
    private Long rideId;
    private float estimatedCost;
}
