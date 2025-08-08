//package com.mediroute.controller;
//
//import com.mediroute.repository.RideRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import javax.sql.DataSource;
//
//@RestController
//@RequestMapping("/api/health")
//public class HealthController {
//
//    @Autowired
//    private DataSource dataSource;
//
//    @Autowired
//    private RideRepository rideRepository;
//
//    @GetMapping("/status")
//    public HealthStatus getHealthStatus() {
//        HealthStatus status = new HealthStatus();
//
//        // Check database
//        try {
//            dataSource.getConnection().close();
//            status.setDatabase("UP");
//        } catch (Exception e) {
//            status.setDatabase("DOWN: " + e.getMessage());
//        }
//
//        // Check OSRM service
//        try {
//            restTemplate.getForObject(osrmBaseUrl + "/health", String.class);
//            status.setOsrmService("UP");
//        } catch (Exception e) {
//            status.setOsrmService("DOWN: " + e.getMessage());
//        }
//
//        // Check data integrity
//        long unassignedCount = rideRepository.countUnassignedRidesForToday();
//        status.setUnassignedRides(unassignedCount);
//
//        return status;
//    }
//}