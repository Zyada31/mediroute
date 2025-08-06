//package com.mediroute.unit.service;
//
//import com.mediroute.dto.RideStatus;
//import com.mediroute.entity.Ride;
//import com.mediroute.service.ride.RideOptimizerService;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.ActiveProfiles;
//
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertFalse;
//
//@SpringBootTest
//@ActiveProfiles("test")
//public class RideOptimizerServiceTest {
//
//    @Autowired
//    private RideOptimizerService optimizer;
//
//    @Test
//    public void testOptimizeAndReturnOrdered() {
//        List<Ride> rides = new ArrayList<>();
//
//        // 1. Create dummy rides with different pickup times
//        rides.add(createRide(1L, 1.0f, "2025-07-26T08:00:00"));
//        rides.add(createRide(2L, 2.0f, "2025-07-26T09:00:00"));
//        rides.add(createRide(3L, 0.5f, "2025-07-26T07:30:00"));
//
//        // 2. Optimize
//        List<Ride> optimized = optimizer.optimizeSchedule(rides);
//
//        // 3. Assert
//        assertFalse(optimized.isEmpty(), "Expected optimized list to not be empty");
//    assertEquals(3, optimized.size(), "Expected 3 rides in optimized list");
//
//        System.out.println("Ride Order:");
//        optimized.forEach(r -> System.out.println("Ride " + r.getId() + " @ " + r.getPickupTime()));
//}
//
//    private Ride createRide(Long id, float distance, String pickupTimeStr) {
//        Ride ride = new Ride();
//        ride.setId(id);
//        ride.setDistance(distance);
//        ride.setPickupTime(LocalDateTime.parse(pickupTimeStr));
//        ride.setStatus(RideStatus.SCHEDULED);
//        return ride;
//    }
//}
