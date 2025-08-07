package com.mediroute.controller;

import com.mediroute.dto.DriverDTO;
import com.mediroute.entity.Driver;
import com.mediroute.service.driver.DriverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@CrossOrigin(origins = "http://localhost:3000")
@Slf4j
@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService assignmentService;

    /**
     * Create or update a single driver.
     */
    @PostMapping
    public ResponseEntity<?> createOrUpdateDriver(
            @RequestBody DriverDTO driverRequest,
            @RequestParam(defaultValue = "false") boolean update
    ) {
        try {
            Driver saved = assignmentService.createOrUpdateDriver(driverRequest, update);
            return ResponseEntity.ok(saved);
        } catch (IllegalStateException e) {
            log.warn("❌ Duplicate driver: {}", driverRequest.getName());
            return ResponseEntity.status(HttpStatus.CONFLICT).body("❌ " + e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("❌ Invalid driver data for {}", driverRequest.getName(), e);
            return ResponseEntity.badRequest().body("❌ " + e.getMessage());
        }
    }

    /**
     * Batch create/update multiple drivers (all go through the service).
     */
    @PostMapping("/batch")
    public ResponseEntity<?> addMultipleDrivers(
            @RequestBody List<DriverDTO> driverRequests,
            @RequestParam(defaultValue = "false") boolean update
    ) {
        List<Driver> savedDrivers = new ArrayList<>();
        for (DriverDTO dto : driverRequests) {
            try {
                savedDrivers.add(assignmentService.createOrUpdateDriver(dto, update));
            } catch (IllegalStateException e) {
                log.warn("⚠️ Skipped duplicate driver: {}", dto.getName());
            } catch (IllegalArgumentException e) {
                log.error("❌ Failed to process driver {}", dto.getName(), e);
            }
        }
        return ResponseEntity.ok(savedDrivers);
    }

    /**
     * List all drivers.
     */
    @GetMapping
    public List<Driver> listAllDrivers() {
        return assignmentService.listAllDrivers();
    }
}
