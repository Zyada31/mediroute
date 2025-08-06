package com.mediroute.dto;

import com.mediroute.entity.Ride;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class RideCategorization {
        private List<Ride> emergencyRides = new ArrayList<>();
        private Map<VehicleTypeEnum, List<Ride>> roundTripRidesByVehicleType = new HashMap<>();
        private Map<VehicleTypeEnum, List<Ride>> oneWayRidesByVehicleType = new HashMap<>();

        public void addEmergencyRide(Ride ride) {
            emergencyRides.add(ride);
        }

        public void addRoundTripRide(VehicleTypeEnum vehicleType, Ride ride) {
            roundTripRidesByVehicleType.computeIfAbsent(vehicleType, k -> new ArrayList<>()).add(ride);
        }

        public void addOneWayRide(VehicleTypeEnum vehicleType, Ride ride) {
            oneWayRidesByVehicleType.computeIfAbsent(vehicleType, k -> new ArrayList<>()).add(ride);
        }

        public int getWheelchairRideCount() {
            return getRideCountForVehicleTypes(VehicleTypeEnum.getWheelchairCompatibleTypes());
        }

        public int getStretcherRideCount() {
            return getRideCountForVehicleTypes(VehicleTypeEnum.getStretcherCompatibleTypes());
        }

        private int getRideCountForVehicleTypes(List<VehicleTypeEnum> vehicleTypes) {
            return vehicleTypes.stream()
                    .mapToInt(type ->
                            roundTripRidesByVehicleType.getOrDefault(type, List.of()).size() +
                                    oneWayRidesByVehicleType.getOrDefault(type, List.of()).size())
                    .sum();
        }

        public int getRoundTripRideCount() {
            return roundTripRidesByVehicleType.values().stream().mapToInt(List::size).sum();
        }

        // Convert to string-based categorization for backward compatibility
//        public RideCategorization toStringBased() {
//            RideCategorization stringBased = new RideCategorization();
//
//            // Copy emergency rides
//            stringBased.getEmergencyRides().addAll(this.emergencyRides);
//
//            // Convert enum-based maps to string-based maps
//            for (Map.Entry<VehicleTypeEnum, List<Ride>> entry : roundTripRidesByVehicleType.entrySet()) {
//                stringBased.getRoundTripRidesByVehicleType().put(VehicleTypeEnum.valueOf(entry.getKey().name()), entry.getValue());
//            }
//
//            for (Map.Entry<VehicleTypeEnum, List<Ride>> entry : oneWayRidesByVehicleType.entrySet()) {
//                stringBased.getOneWayRidesByVehicleType().put(VehicleTypeEnum.valueOf(entry.getKey().name()), entry.getValue());
//            }
//
//            return stringBased;
//        }
    }