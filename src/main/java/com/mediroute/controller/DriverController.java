package com.mediroute.controller;

import com.mediroute.dto.DriverDTO;
import com.mediroute.dto.DriverStatisticsDTO;
import com.mediroute.dto.RideDetailDTO;
import com.mediroute.entity.Driver;
import com.mediroute.entity.Ride;
import com.mediroute.service.driver.DriverService;
import com.mediroute.service.ride.RideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = {"http://localhost:3000", "https://app.mediroute.com"})
@RestController
@RequestMapping("/api/v1/drivers")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Driver Management", description = "Driver operations and management")
public class DriverController {

    private final DriverService driverService;
    private final RideService rideService;

    // ========== CREATE/UPDATE OPERATIONS ==========

    @Operation(summary = "Create a new driver", description = "Create a new driver with medical transport capabilities")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Driver created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid driver data"),
            @ApiResponse(responseCode = "409", description = "Driver already exists")
    })
    @PostMapping
    public ResponseEntity<?> createDriver(@Validated(DriverDTO.Create.class) @RequestBody DriverDTO dto) {
        try {
            Driver saved = driverService.createOrUpdateDriver(dto, false);
            DriverDTO response = DriverDTO.fromEntity(saved);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalStateException e) {
            log.warn("Duplicate driver: {}", dto.getName());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(createErrorResponse("DUPLICATE_DRIVER", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.error("Invalid driver data for {}", dto.getName(), e);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("INVALID_DATA", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error creating driver: {}", dto.getName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to create driver"));
        }
    }

    @Operation(summary = "Update an existing driver", description = "Update driver information and capabilities")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Driver updated successfully"),
            @ApiResponse(responseCode = "404", description = "Driver not found"),
            @ApiResponse(responseCode = "400", description = "Invalid update data")
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> updateDriver(
            @Parameter(description = "Driver ID") @PathVariable Long id,
            @Validated(DriverDTO.Update.class) @RequestBody DriverDTO dto) {
        try {
            var existingDriver = driverService.getDriverById(id);
            if (existingDriver.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // Set the ID for update
            dto.setId(id);
            Driver updated = driverService.createOrUpdateDriver(dto, true);

            DriverDTO response = DriverDTO.fromEntity(updated);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid update data for driver {}", id, e);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("INVALID_DATA", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error updating driver {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to update driver"));
        }
    }

    // ========== BATCH OPERATIONS ==========

    @Operation(summary = "Batch create drivers", description = "Create multiple drivers in a single operation")
    @PostMapping("/batch")
    public ResponseEntity<BatchDriverResponse> createMultipleDrivers(
            @Valid @RequestBody List<DriverDTO> createRequests) {

        log.info("Processing batch creation of {} drivers", createRequests.size());
        BatchDriverResponse response = new BatchDriverResponse();

        for (DriverDTO createRequest : createRequests) {
            try {
                Driver saved = driverService.createOrUpdateDriver(createRequest, false);
                response.addSuccess(DriverDTO.fromEntity(saved));

            } catch (IllegalStateException e) {
                log.warn("⚠️ Skipped duplicate driver: {}", createRequest.getName());
                response.addError(createRequest.getName(), "DUPLICATE_DRIVER", e.getMessage());
            } catch (IllegalArgumentException e) {
                log.error("Failed to process driver: {}", createRequest.getName(), e);
                response.addError(createRequest.getName(), "INVALID_DATA", e.getMessage());
            } catch (Exception e) {
                log.error("Unexpected error for driver: {}", createRequest.getName(), e);
                response.addError(createRequest.getName(), "INTERNAL_ERROR", "Failed to create driver");
            }
        }

        log.info("Batch processing complete: {} successful, {} failed",
                response.getSuccessful().size(), response.getErrors().size());

        return ResponseEntity.ok(response);
    }

    // ========== RETRIEVAL OPERATIONS ==========

    @Operation(summary = "Get all drivers", description = "Retrieve all drivers with their details")
    @GetMapping
    public ResponseEntity<List<DriverDTO>> getAllDrivers() {
        try {
            List<Driver> drivers = driverService.listAllDrivers();
            List<DriverDTO> driverDTOs = drivers.stream()
                    .map(DriverDTO::fromEntity)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(driverDTOs);
        } catch (Exception e) {
            log.error("Error retrieving drivers", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Get driver by ID", description = "Retrieve a specific driver by their ID")
    @GetMapping("/{id}")
    public ResponseEntity<DriverDTO> getDriverById(
            @Parameter(description = "Driver ID") @PathVariable Long id) {
        try {
            return driverService.getDriverById(id)
                    .map(DriverDTO::fromEntity)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error retrieving driver {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Get active drivers", description = "Retrieve only active drivers")
    @GetMapping("/active")
    public ResponseEntity<List<DriverDTO>> getActiveDrivers() {
        try {
            List<Driver> activeDrivers = driverService.getQualifiedDrivers();
            List<DriverDTO> driverDTOs = activeDrivers.stream()
                    .map(DriverDTO::fromEntity)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(driverDTOs);
        } catch (Exception e) {
            log.error("Error retrieving active drivers", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Get drivers by capability", description = "Get drivers filtered by medical transport capabilities")
    @GetMapping("/by-capability")
    public ResponseEntity<List<DriverDTO>> getDriversByCapability(
            @Parameter(description = "Vehicle type") @RequestParam(required = false) String vehicleType,
            @Parameter(description = "Wheelchair accessible") @RequestParam(required = false) Boolean wheelchairAccessible,
            @Parameter(description = "Stretcher capable") @RequestParam(required = false) Boolean stretcherCapable,
            @Parameter(description = "Oxygen equipped") @RequestParam(required = false) Boolean oxygenEquipped) {
        try {
            List<Driver> drivers = driverService.getQualifiedDrivers().stream()
                    .filter(driver -> vehicleType == null ||
                            driver.getVehicleType().name().equalsIgnoreCase(vehicleType))
                    .filter(driver -> wheelchairAccessible == null ||
                            driver.getWheelchairAccessible().equals(wheelchairAccessible))
                    .filter(driver -> stretcherCapable == null ||
                            driver.getStretcherCapable().equals(stretcherCapable))
                    .filter(driver -> oxygenEquipped == null ||
                            driver.getOxygenEquipped().equals(oxygenEquipped))
                    .collect(Collectors.toList());

            List<DriverDTO> driverDTOs = drivers.stream()
                    .map(DriverDTO::fromEntity)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(driverDTOs);
        } catch (Exception e) {
            log.error("Error filtering drivers by capability", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== RIDES BY DRIVER ==========

    @Operation(summary = "Get rides by driver and date",
            description = "Retrieve all rides assigned to a specific driver on a given date")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rides retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Driver not found")
    })
    @GetMapping("/{id}/rides")
    public ResponseEntity<List<RideDetailDTO>> getRidesByDriverAndDate(
            @Parameter(description = "Driver ID") @PathVariable Long id,
            @Parameter(description = "Date (YYYY-MM-DD)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            // Verify driver exists
            var driver = driverService.getDriverById(id);
            if (driver.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // Get rides for this driver on the specified date
            List<Ride> rides = rideService.findRidesByDriver(id, date);

            // Convert to DTOs
            List<RideDetailDTO> rideDTOs = rides.stream()
                    .map(RideDetailDTO::fromEntity)
                    .sorted((r1, r2) -> {
                        // Sort by pickup time
                        if (r1.getPickupTime() != null && r2.getPickupTime() != null) {
                            return r1.getPickupTime().compareTo(r2.getPickupTime());
                        }
                        return 0;
                    })
                    .collect(Collectors.toList());

            log.info("Found {} rides for driver {} on {}", rideDTOs.size(), id, date);
            return ResponseEntity.ok(rideDTOs);

        } catch (Exception e) {
            log.error("Error retrieving rides for driver {} on date {}", id, date, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Get today's rides for driver",
            description = "Retrieve all rides assigned to a specific driver for today")
    @GetMapping("/{id}/rides/today")
    public ResponseEntity<List<RideDetailDTO>> getTodayRidesForDriver(
            @Parameter(description = "Driver ID") @PathVariable Long id) {
        return getRidesByDriverAndDate(id, LocalDate.now());
    }

    @Operation(summary = "Get driver's ride history",
            description = "Retrieve ride history for a driver within a date range")
    @GetMapping("/{id}/rides/history")
    public ResponseEntity<List<RideDetailDTO>> getDriverRideHistory(
            @Parameter(description = "Driver ID") @PathVariable Long id,
            @Parameter(description = "Start date")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            // Verify driver exists
            var driver = driverService.getDriverById(id);
            if (driver.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            List<RideDetailDTO> allRides = new ArrayList<>();

            // Iterate through each day in the range
            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                List<Ride> dayRides = rideService.findRidesByDriver(id, currentDate);
                allRides.addAll(dayRides.stream()
                        .map(RideDetailDTO::fromEntity)
                        .collect(Collectors.toList()));
                currentDate = currentDate.plusDays(1);
            }

            // Sort by pickup time
            allRides.sort((r1, r2) -> {
                if (r1.getPickupTime() != null && r2.getPickupTime() != null) {
                    return r1.getPickupTime().compareTo(r2.getPickupTime());
                }
                return 0;
            });

            log.info("Found {} rides for driver {} between {} and {}",
                    allRides.size(), id, startDate, endDate);
            return ResponseEntity.ok(allRides);

        } catch (Exception e) {
            log.error("Error retrieving ride history for driver {} between {} and {}",
                    id, startDate, endDate, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== WORKLOAD AND AVAILABILITY ==========

    @Operation(summary = "Get driver workload", description = "Get driver workload for a specific date")
    @GetMapping("/{id}/workload")
    public ResponseEntity<?> getDriverWorkload(
            @Parameter(description = "Driver ID") @PathVariable Long id,
            @Parameter(description = "Date (YYYY-MM-DD)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            var workload = driverService.getDriverWorkload(id, date);
            return ResponseEntity.ok(workload);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error getting workload for driver {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Get all driver workloads",
            description = "Get workload summary for all drivers on a specific date")
    @GetMapping("/workload")
    public ResponseEntity<List<DriverService.DriverWorkload>> getAllDriverWorkloads(
            @Parameter(description = "Date (YYYY-MM-DD)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            List<Driver> activeDrivers = driverService.getQualifiedDrivers();
            List<DriverService.DriverWorkload> workloads = activeDrivers.stream()
                    .map(driver -> driverService.getDriverWorkload(driver.getId(), date))
                    .sorted((w1, w2) -> Double.compare(w2.getUtilizationRate(), w1.getUtilizationRate()))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(workloads);
        } catch (Exception e) {
            log.error("Error getting all driver workloads for {}", date, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== STATISTICS ==========

    @Operation(summary = "Get driver statistics", description = "Get comprehensive driver statistics")
    @GetMapping("/statistics")
    public ResponseEntity<DriverStatisticsDTO> getDriverStatistics() {
        try {
            var stats = driverService.getDriverStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting driver statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== MANAGEMENT OPERATIONS ==========

    @Operation(summary = "Deactivate driver", description = "Deactivate a driver")
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivateDriver(
            @Parameter(description = "Driver ID") @PathVariable Long id) {
        try {
            driverService.deactivateDriver(id);
            return ResponseEntity.ok(createSuccessResponse("Driver deactivated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error deactivating driver {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to deactivate driver"));
        }
    }

    @Operation(summary = "Reactivate driver", description = "Reactivate a driver")
    @PutMapping("/{id}/reactivate")
    public ResponseEntity<?> reactivateDriver(
            @Parameter(description = "Driver ID") @PathVariable Long id) {
        try {
            driverService.reactivateDriver(id);
            return ResponseEntity.ok(createSuccessResponse("Driver reactivated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("INVALID_STATE", e.getMessage()));
        } catch (Exception e) {
            log.error("Error reactivating driver {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to reactivate driver"));
        }
    }

    // ========== HELPER METHODS ==========

    private ErrorResponse createErrorResponse(String code, String message) {
        return new ErrorResponse(code, message, LocalDateTime.now());
    }

    private SuccessResponse createSuccessResponse(String message) {
        return new SuccessResponse(message, LocalDateTime.now());
    }

    // ========== RESPONSE CLASSES ==========

    public static class BatchDriverResponse {
        private List<DriverDTO> successful = new ArrayList<>();
        private List<DriverError> errors = new ArrayList<>();

        public void addSuccess(DriverDTO driver) {
            successful.add(driver);
        }

        public void addError(String driverName, String errorCode, String message) {
            errors.add(new DriverError(driverName, errorCode, message));
        }

        // Getters
        public List<DriverDTO> getSuccessful() { return successful; }
        public List<DriverError> getErrors() { return errors; }
        public int getSuccessCount() { return successful.size(); }
        public int getErrorCount() { return errors.size(); }
    }

    public static class DriverError {
        private String driverName;
        private String errorCode;
        private String message;
        private LocalDateTime timestamp = LocalDateTime.now();

        public DriverError(String driverName, String errorCode, String message) {
            this.driverName = driverName;
            this.errorCode = errorCode;
            this.message = message;
        }

        // Getters
        public String getDriverName() { return driverName; }
        public String getErrorCode() { return errorCode; }
        public String getMessage() { return message; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }

    public static class ErrorResponse {
        private String errorCode;
        private String message;
        private LocalDateTime timestamp;

        public ErrorResponse(String errorCode, String message, LocalDateTime timestamp) {
            this.errorCode = errorCode;
            this.message = message;
            this.timestamp = timestamp;
        }

        // Getters
        public String getErrorCode() { return errorCode; }
        public String getMessage() { return message; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }

    public static class SuccessResponse {
        private String message;
        private LocalDateTime timestamp;

        public SuccessResponse(String message, LocalDateTime timestamp) {
            this.message = message;
            this.timestamp = timestamp;
        }

        // Getters
        public String getMessage() { return message; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}

