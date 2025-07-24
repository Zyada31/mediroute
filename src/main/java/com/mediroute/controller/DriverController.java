package com.mediroute.controller;

import com.mediroute.entity.Driver;
import com.mediroute.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@CrossOrigin(origins = "http://localhost:3000")
@Slf4j
@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final DriverRepository driverRepository;

    @PostMapping
    public ResponseEntity<Driver> createDriver(@RequestBody Driver driver) {
        log.info("🚐 Creating new driver: {}", driver.getName());

        // Validate email uniqueness (optional safety check)
        if (driverRepository.existsByEmail(driver.getEmail())) {
            return ResponseEntity.badRequest().body(null);
        }

        Driver saved = driverRepository.save(driver);
        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public List<Driver> listAllDrivers() {
        return driverRepository.findAll();
    }

    @PostMapping("/batch")
    public ResponseEntity<List<Driver>> addMultipleDrivers(@RequestBody List<Driver> drivers) {
        List<Driver> savedDrivers = driverRepository.saveAll(drivers);
        return ResponseEntity.ok(savedDrivers);
    }
}