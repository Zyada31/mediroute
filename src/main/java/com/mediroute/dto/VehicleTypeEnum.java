package com.mediroute.dto;

import com.mediroute.entity.Patient;
import com.mediroute.entity.Ride;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.*;
import java.util.stream.Collectors;

@Schema(description = "Vehicle types available")
public enum VehicleTypeEnum {
    @Schema(description = "Standard sedan")
    SEDAN,

    @Schema(description = "SUV or larger car")
    SUV,

    @Schema(description = "Standard van")
    VAN,

    @Schema(description = "Wheelchair accessible van")
    WHEELCHAIR_VAN,

    @Schema(description = "Stretcher capable van")
    STRETCHER_VAN,

    @Schema(description = "Full ambulance")
    AMBULANCE
}

