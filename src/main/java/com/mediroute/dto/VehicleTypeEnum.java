package com.mediroute.dto;

import com.mediroute.entity.Patient;
import com.mediroute.entity.Ride;

import java.util.*;
import java.util.stream.Collectors;

public enum VehicleTypeEnum {
    SEDAN("Standard sedan vehicle", false, false, false, 4),
    VAN("Standard van vehicle", false, false, false, 6),
    WHEELCHAIR_VAN("Wheelchair accessible van", true, false, true, 4),
    STRETCHER_VAN("Medical stretcher van", true, true, true, 2),
    AMBULANCE("Full medical ambulance", true, true, true, 1);

    private final String description;
    private final boolean wheelchairAccessible;
    private final boolean stretcherCapable;
    private final boolean oxygenEquipped;
    private final int defaultCapacity;

    VehicleTypeEnum(String description, boolean wheelchairAccessible,
                    boolean stretcherCapable, boolean oxygenEquipped,
                    int defaultCapacity) {
        this.description = description;
        this.wheelchairAccessible = wheelchairAccessible;
        this.stretcherCapable = stretcherCapable;
        this.oxygenEquipped = oxygenEquipped;
        this.defaultCapacity = defaultCapacity;
    }

    // Getters
    public String getDescription() {
        return description;
    }

    public boolean isWheelchairAccessible() {
        return wheelchairAccessible;
    }

    public boolean isStretcherCapable() {
        return stretcherCapable;
    }

    public boolean isOxygenEquipped() {
        return oxygenEquipped;
    }

    public int getDefaultCapacity() {
        return defaultCapacity;
    }

    // Medical transport compatibility methods
    public boolean canHandlePatient(Patient patient) {
        if (patient == null) return true;

        // Check wheelchair requirement
        if (Boolean.TRUE.equals(patient.getRequiresWheelchair()) && !this.wheelchairAccessible) {
            return false;
        }

        // Check stretcher requirement
        if (Boolean.TRUE.equals(patient.getRequiresStretcher()) && !this.stretcherCapable) {
            return false;
        }

        // Check oxygen requirement
        if (Boolean.TRUE.equals(patient.getRequiresOxygen()) && !this.oxygenEquipped) {
            return false;
        }

        // Check mobility level compatibility
        if (patient.getMobilityLevel() != null) {
            switch (patient.getMobilityLevel()) {
                case STRETCHER:
                    return this.stretcherCapable;
                case WHEELCHAIR:
                    return this.wheelchairAccessible;
                case ASSISTED:
                case INDEPENDENT:
                    return true; // Any vehicle can handle these
            }
        }

        return true;
    }

    public boolean canHandleMobilityLevel(MobilityLevel mobilityLevel) {
        if (mobilityLevel == null) return true;

        return switch (mobilityLevel) {
            case STRETCHER -> this.stretcherCapable;
            case WHEELCHAIR -> this.wheelchairAccessible;
            case ASSISTED, INDEPENDENT -> true;
        };
    }

    // Static utility methods
    public static VehicleTypeEnum getRequiredVehicleType(Patient patient) {
        if (patient == null) return SEDAN;

        // Check for stretcher requirement (highest priority)
        if (Boolean.TRUE.equals(patient.getRequiresStretcher()) ||
                patient.getMobilityLevel() == MobilityLevel.STRETCHER) {
            return STRETCHER_VAN;
        }

        // Check for wheelchair requirement
        if (Boolean.TRUE.equals(patient.getRequiresWheelchair()) ||
                patient.getMobilityLevel() == MobilityLevel.WHEELCHAIR) {
            return WHEELCHAIR_VAN;
        }

        // Check for oxygen requirement (usually needs medical vehicle)
        if (Boolean.TRUE.equals(patient.getRequiresOxygen())) {
            return WHEELCHAIR_VAN; // Assume oxygen equipment is in wheelchair vans
        }

        // Default to sedan for independent/assisted patients
        return SEDAN;
    }

    public static VehicleTypeEnum getRequiredVehicleType(Ride ride) {
        if (ride == null || ride.getPatient() == null) return SEDAN;

        // Check explicit vehicle requirement first
        if (ride.getRequiredVehicleType() != null) {
            try {
                return ride.getRequiredVehicleType();
            } catch (IllegalArgumentException e) {
                // Fall back to patient-based determination
            }
        }

        return getRequiredVehicleType(ride.getPatient());
    }

    public static List<VehicleTypeEnum> getMedicalTransportTypes() {
        return List.of(WHEELCHAIR_VAN, STRETCHER_VAN, AMBULANCE);
    }

    public static List<VehicleTypeEnum> getRegularTransportTypes() {
        return List.of(SEDAN, VAN);
    }

    public static List<VehicleTypeEnum> getWheelchairCompatibleTypes() {
        return Arrays.stream(values())
                .filter(VehicleTypeEnum::isWheelchairAccessible)
                .collect(Collectors.toList());
    }

    public static List<VehicleTypeEnum> getStretcherCompatibleTypes() {
        return Arrays.stream(values())
                .filter(VehicleTypeEnum::isStretcherCapable)
                .collect(Collectors.toList());
    }

    // Priority ordering for medical transport (higher priority vehicles first)
    public static List<VehicleTypeEnum> getByMedicalPriority() {
        return List.of(AMBULANCE, STRETCHER_VAN, WHEELCHAIR_VAN, VAN, SEDAN);
    }

    // Check if this vehicle type can substitute for another
    public boolean canSubstituteFor(VehicleTypeEnum other) {
        if (other == null) return true;
        if (this == other) return true;

        // Higher capability vehicles can substitute for lower ones
        return switch (other) {
            case SEDAN -> this != SEDAN; // Any vehicle can substitute for sedan
            case VAN -> this == WHEELCHAIR_VAN || this == STRETCHER_VAN || this == AMBULANCE;
            case WHEELCHAIR_VAN -> this == STRETCHER_VAN || this == AMBULANCE;
            case STRETCHER_VAN -> this == AMBULANCE;
            case AMBULANCE -> false; // Nothing can substitute for ambulance
        };
    }

    // Get upgrade path (what vehicles can handle if this one is not available)
    public List<VehicleTypeEnum> getUpgradeOptions() {
        return Arrays.stream(values())
                .filter(type -> type.canSubstituteFor(this) && type != this)
                .collect(Collectors.toList());
    }

    // Display methods
    public String getDisplayName() {
        return switch (this) {
            case SEDAN -> "Sedan";
            case VAN -> "Van";
            case WHEELCHAIR_VAN -> "Wheelchair Van";
            case STRETCHER_VAN -> "Stretcher Van";
            case AMBULANCE -> "Ambulance";
        };
    }

    public String getCapabilityDescription() {
        List<String> capabilities = new ArrayList<>();

        if (wheelchairAccessible) capabilities.add("Wheelchair Accessible");
        if (stretcherCapable) capabilities.add("Stretcher Capable");
        if (oxygenEquipped) capabilities.add("Oxygen Equipped");

        capabilities.add(defaultCapacity + " passenger capacity");

        return String.join(", ", capabilities);
    }

    @Override
    public String toString() {
        return getDisplayName() + " (" + getCapabilityDescription() + ")";
    }

    // JSON serialization helper
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", this.name());
        map.put("displayName", getDisplayName());
        map.put("description", description);
        map.put("wheelchairAccessible", wheelchairAccessible);
        map.put("stretcherCapable", stretcherCapable);
        map.put("oxygenEquipped", oxygenEquipped);
        map.put("defaultCapacity", defaultCapacity);
        return map;
    }
}
