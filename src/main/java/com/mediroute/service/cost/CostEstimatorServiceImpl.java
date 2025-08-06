//package com.mediroute.service.cost;
//
//import com.mediroute.dto.RideCostEstimate;
//import com.mediroute.entity.Ride;
//import com.mediroute.repository.RideRepository;
//import jakarta.transaction.Transactional;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class CostEstimatorServiceImpl implements CostEstimatorService {
//
//    private final RideRepository rideRepository;
//
//    @Value("${cost.baseRatePerMile:2.50}")
//    private float baseRatePerMile;
//
//    @Value("${cost.wheelchairSurcharge:10.0}")
//    private float wheelchairSurcharge;
//
//    @Value("${cost.nightSurcharge:5.0}")
//    private float nightSurcharge;
//
//    @Value("${cost.nightStartHour:20}")
//    private int nightStart;
//
//    @Value("${cost.nightEndHour:6}")
//    private int nightEnd;
//
//    @Override
//    public RideCostEstimate estimateCost(Ride ride) {
//        if (ride.getDistance() == null) {
//            log.warn("🚨 Ride distance is null for rideId={}, defaulting to 0", ride.getId());
//            return new RideCostEstimate(ride.getId(), 0f);
//        }
//
//        float cost = ride.getDistance() * baseRatePerMile;
//
//        if (ride.getRequiredVehicleType() != null &&
//                ride.getRequiredVehicleType().toLowerCase().contains("wheelchair")) {
//            cost += wheelchairSurcharge;
//        }
//
//        if (ride.getPickupTime() != null) {
//            int hour = ride.getPickupTime().getHour();
//            if (hour < nightEnd || hour > nightStart) {
//                cost += nightSurcharge;
//            }
//        } else {
//            log.warn("❗ Ride pickupTime is null for rideId={}, skipping night surcharge", ride.getId());
//        }
//
//        float rounded = Math.round(cost * 100f) / 100f;
//        return new RideCostEstimate(ride.getId(), rounded);
//    }
//
//    @Transactional
//    public void estimateAndPersistCost(Long rideId) {
//        Ride ride = rideRepository.findById(rideId)
//                .orElseThrow(() -> new RuntimeException("Ride not found: " + rideId));
//        float cost = estimateCost(ride).getEstimatedCost();
//        ride.setEstimatedCost(cost);
//        rideRepository.save(ride);
//        log.info("💰 Estimated cost ${} saved for rideId={}", cost, rideId);
//    }
//
//    @Transactional
//    public void estimateAllCosts() {
//        List<Ride> rides = rideRepository.findAll();
//        for (Ride ride : rides) {
//            float cost = estimateCost(ride).getEstimatedCost();
//            ride.setEstimatedCost(cost);
//        }
//        rideRepository.saveAll(rides);
//        log.info("💰 Bulk cost estimation completed for {} rides", rides.size());
//    }
//}