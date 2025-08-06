//package com.mediroute.controller;
//
//import com.mediroute.dto.RideCostEstimate;
//import com.mediroute.entity.Ride;
//import com.mediroute.repository.RideRepository;
//import com.mediroute.service.cost.CostEstimatorServiceImpl;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//@RestController
//@RequestMapping("/api/cost")
//@RequiredArgsConstructor
//@Slf4j
//public class CostEstimationController {
//
//    private final CostEstimatorServiceImpl costEstimatorService;
//    private final RideRepository rideRepository;
//
//    /**
//     * Estimate cost for a specific ride without saving to DB
//     */
//    @GetMapping("/preview/{rideId}")
//    public RideCostEstimate previewCost(@PathVariable Long rideId) {
//        Ride ride = rideRepository.findById(rideId)
//                .orElseThrow(() -> new RuntimeException("Ride not found with ID: " + rideId));
//        return costEstimatorService.estimateCost(ride);
//    }
//
//    /**
//     * Estimate cost and persist the value into the ride's estimatedCost field
//     */
//    @PostMapping("/estimate/{rideId}")
//    public RideCostEstimate estimateAndSave(@PathVariable Long rideId) {
//        costEstimatorService.estimateAndPersistCost(rideId);
//        Ride updatedRide = rideRepository.findById(rideId)
//                .orElseThrow(() -> new RuntimeException("Ride not found after update: " + rideId));
//        return new RideCostEstimate(rideId, updatedRide.getEstimatedCost());
//    }
//
//    /**
//     * Bulk estimation & save for all rides
//     */
//    @PostMapping("/estimate-all")
//    public String estimateAllAndSave() {
//        costEstimatorService.estimateAllCosts();
//        return "✅ All ride costs estimated and saved.";
//    }
//
//    /**
//     * Preview cost estimates for all rides without saving
//     */
//    @GetMapping("/preview-all")
//    public List<RideCostEstimate> previewAll() {
//        return rideRepository.findAll().stream()
//                .map(costEstimatorService::estimateCost)
//                .collect(Collectors.toList());
//    }
//}