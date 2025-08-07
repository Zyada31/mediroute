package com.mediroute.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.EnumSet;

@Schema(description = "Ride status")
public enum RideStatus {
    @Schema(description = "Ride is scheduled but not assigned")
    SCHEDULED,

    @Schema(description = "Ride is assigned to driver(s)")
    ASSIGNED,

    @Schema(description = "Driver is en route to pickup")
    EN_ROUTE_PICKUP,

    @Schema(description = "Driver has arrived for pickup")
    ARRIVED_PICKUP,

    @Schema(description = "Patient is in vehicle, en route to destination")
    EN_ROUTE_DROPOFF,

    @Schema(description = "Patient has been dropped off")
    ARRIVED_DROPOFF,

    @Schema(description = "Ride is completed")
    COMPLETED,

    @Schema(description = "Ride was cancelled")
    CANCELLED,

    @Schema(description = "Ride was not completed due to no-show or other issues")
    NO_SHOW
}