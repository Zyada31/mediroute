package com.mediroute.service.cost;

import com.mediroute.dto.RideCostEstimate;
import com.mediroute.entity.Ride;

public interface CostEstimatorService {
    RideCostEstimate estimateCost(Ride ride);
}