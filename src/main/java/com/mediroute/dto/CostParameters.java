package com.mediroute.dto;

public record CostParameters(
        double baseFare,              // e.g., $5.00
        double perMileRate,           // e.g., $2.00/mile
        double wheelchairSurcharge,   // e.g., $10.00
        double waitTimePerMinute      // e.g., $0.50/min
) {}
