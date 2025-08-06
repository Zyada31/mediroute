package com.mediroute.dto;

// New Enums
public enum RideType {
    ONE_WAY,           // Single trip from A to B
    ROUND_TRIP,        // A to B, wait, B to A (same driver)
    PICKUP_ONLY,       // Just pickup leg of a longer journey
    DROPOFF_ONLY       // Just dropoff leg of a longer journey
}