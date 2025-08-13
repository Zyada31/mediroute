// Enhanced DriverController.java with additional endpoints
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    // ========== NEW ENDPOINTS ==========

    /**
     * Get rides by driver ID or all rides if no driver ID provided
     */
    @Operation(
            summary = "Get rides by driver or all rides",
            description = "Retrieve rides for a specific driver ID or all rides if no driver ID provided"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rides retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Driver not found (if driver ID provided)")
    })
    @PreAuthorize(""" 
      hasAnyRole('ADMIN','DISPATCHER') or
      (#driverId != null and #driverId == authentication.details['driverId'])
    """)
    @GetMapping("/rides")
    public ResponseEntity<List<RideDetailDTO>> getRidesByDriverOrAll(
            @Parameter(description = "Driver ID (optional - if not provided, returns all rides)")
            @RequestParam(required = false) Long driverId,
            @Parameter(description = "Date (YYYY-MM-DD) - defaults to today")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        if (date == null) {
            date = LocalDate.now();
        }

        try {
            List<RideDetailDTO> rides;

            if (driverId != null) {
                // Verify driver exists
                Optional<Driver> driver = driverService.getDriverById(driverId);
                if (driver.isEmpty()) {
                    log.warn("Driver not found with ID: {}", driverId);
                    return ResponseEntity.notFound().build();
                }

                // Get rides for specific driver
                List<Ride> driverRides = rideService.findRidesByDriver(driverId, date);
                rides = driverRides.stream()
                        .map(RideDetailDTO::fromEntity)
                        .sorted((r1, r2) -> {
                            if (r1.getPickupTime() != null && r2.getPickupTime() != null) {
                                return r1.getPickupTime().compareTo(r2.getPickupTime());
                            }
                            return 0;
                        })
                        .collect(Collectors.toList());

                log.info("Found {} rides for driver {} on {}", rides.size(), driverId, date);
            } else {
                // Get all rides for the date
                rides = rideService.findRidesByDate(date);
                log.info("Found {} total rides on {}", rides.size(), date);
            }

            return ResponseEntity.ok(rides);

        } catch (Exception e) {
            log.error("Error retrieving rides for driver {} on date {}", driverId, date, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get driver by ID or by name (search)
     */
    @Operation(
            summary = "Get driver by ID or search by name",
            description = "Retrieve a specific driver by ID or search drivers by name"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Driver(s) found"),
            @ApiResponse(responseCode = "404", description = "Driver not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request - provide either ID or name")
    })
    @GetMapping("/search")
    public ResponseEntity<?> getDriverByIdOrName(
            @Parameter(description = "Driver ID (mutually exclusive with name)")
            @RequestParam(required = false) Long id,
            @Parameter(description = "Driver name to search (mutually exclusive with id)")
            @RequestParam(required = false) String name) {

        // Validate that exactly one parameter is provided
        if ((id == null && name == null) || (id != null && name != null)) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("INVALID_REQUEST",
                            "Please provide either 'id' or 'name' parameter, but not both"));
        }

        try {
            if (id != null) {
                // Search by ID - return single driver
                Optional<Driver> driver = driverService.getDriverById(id);
                if (driver.isPresent()) {
                    DriverDTO driverDTO = DriverDTO.fromEntity(driver.get());
                    log.info("Found driver by ID {}: {}", id, driver.get().getName());
                    return ResponseEntity.ok(driverDTO);
                } else {
                    log.warn("Driver not found with ID: {}", id);
                    return ResponseEntity.notFound().build();
                }
            } else {
                // Search by name - return list of matching drivers
                List<Driver> drivers = driverService.searchDriversByName(name);
                List<DriverDTO> driverDTOs = drivers.stream()
                        .map(DriverDTO::fromEntity)
                        .collect(Collectors.toList());

                log.info("Found {} drivers matching name '{}'", drivers.size(), name);
                return ResponseEntity.ok(driverDTOs);
            }

        } catch (Exception e) {
            log.error("Error searching for driver with id={}, name={}", id, name, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to search for driver"));
        }
    }

    /**
     * Get driver's daily summary by driver ID
     */
    @Operation(
            summary = "Get driver's daily summary",
            description = "Get comprehensive daily summary for a specific driver including rides, workload, and performance metrics"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Daily summary retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Driver not found")
    })
    @GetMapping("/{id}/daily-summary")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER') or #id == authentication.details['driverId']")
    public ResponseEntity<?> getDriverDailySummary(
            @Parameter(description = "Driver ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "Date (YYYY-MM-DD) - defaults to today")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        if (date == null) {
            date = LocalDate.now();
        }

        try {
            // Verify driver exists
            Optional<Driver> driverOpt = driverService.getDriverById(id);
            if (driverOpt.isEmpty()) {
                log.warn("Driver not found with ID: {}", id);
                return ResponseEntity.notFound().build();
            }

            Driver driver = driverOpt.get();

            // Get rides for the day
            List<Ride> rides = rideService.findRidesByDriver(id, date);
            List<RideDetailDTO> rideDetails = rides.stream()
                    .map(RideDetailDTO::fromEntity)
                    .sorted((r1, r2) -> {
                        if (r1.getPickupTime() != null && r2.getPickupTime() != null) {
                            return r1.getPickupTime().compareTo(r2.getPickupTime());
                        }
                        return 0;
                    })
                    .collect(Collectors.toList());

            // Get workload information
            DriverService.DriverWorkload workload = driverService.getDriverWorkload(id, date);

            // Create comprehensive summary
            DriverDailySummary summary = DriverDailySummary.builder()
                    .driver(DriverDTO.fromEntity(driver))
                    .date(date)
                    .rides(rideDetails)
                    .workload(workload)
                    .summary(createDailySummaryStats(rides, driver, date))
                    .build();

            log.info("Generated daily summary for driver {} ({}): {} rides, {:.1f}% utilization",
                    id, driver.getName(), rides.size(), workload.getUtilizationRate());

            return ResponseEntity.ok(summary);

        } catch (Exception e) {
            log.error("Error generating daily summary for driver {} on date {}", id, date, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to generate daily summary"));
        }
    }

    // ========== EXISTING ENDPOINTS (Keep all your existing methods) ==========

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
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

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public ResponseEntity<?> updateDriver(
            @Parameter(description = "Driver ID") @PathVariable Long id,
            @Validated(DriverDTO.Update.class) @RequestBody DriverDTO dto) {
        try {
            var existingDriver = driverService.getDriverById(id);
            if (existingDriver.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

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

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public ResponseEntity<com.mediroute.dto.PageResponse<DriverDTO>> getAllDrivers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "id,asc") String sort) {
        try {
            String[] sortParts = sort.split(",");
            Sort s = (sortParts.length == 2 && sortParts[1].equalsIgnoreCase("desc"))
                    ? Sort.by(sortParts[0]).descending()
                    : Sort.by(sortParts[0]).ascending();
            Pageable pageable = PageRequest.of(page, size, s);

            Page<Driver> pg = driverService.findAll(pageable);
            List<DriverDTO> items = pg.getContent().stream().map(DriverDTO::fromEntity).toList();
            var resp = new com.mediroute.dto.PageResponse<>(items, pg.getNumber(), pg.getSize(), pg.getTotalElements(), pg.getTotalPages(), sort);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            log.error("Error retrieving drivers", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER') or #id == authentication.details['driverId']")
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

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public ResponseEntity<DriverStatisticsDTO> getDriverStatistics() {
        try {
            var stats = driverService.getDriverStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting driver statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== HELPER METHODS ==========

    /**
     * Create detailed daily summary statistics
     */
    private DailySummaryStats createDailySummaryStats(List<Ride> rides, Driver driver, LocalDate date) {
        int totalRides = rides.size();
        int completedRides = (int) rides.stream()
                .filter(r -> r.getStatus().name().equals("COMPLETED"))
                .count();
        int pendingRides = (int) rides.stream()
                .filter(r -> !r.getStatus().name().equals("COMPLETED") &&
                        !r.getStatus().name().equals("CANCELLED"))
                .count();
        int cancelledRides = (int) rides.stream()
                .filter(r -> r.getStatus().name().equals("CANCELLED"))
                .count();

        double totalDistance = rides.stream()
                .mapToDouble(r -> r.getDistance() != null ? r.getDistance() : 0.0)
                .sum();

        int totalEstimatedMinutes = rides.stream()
                .mapToInt(r -> r.getEstimatedDuration() != null ? r.getEstimatedDuration() : 0)
                .sum();

        long wheelchairRides = rides.stream()
                .filter(r -> "WHEELCHAIR_VAN".equals(r.getRequiredVehicleType()))
                .count();

        long stretcherRides = rides.stream()
                .filter(r -> "STRETCHER_VAN".equals(r.getRequiredVehicleType()))
                .count();

        long emergencyRides = rides.stream()
                .filter(r -> r.getPriority().name().equals("EMERGENCY"))
                .count();

        return DailySummaryStats.builder()
                .totalRides(totalRides)
                .completedRides(completedRides)
                .pendingRides(pendingRides)
                .cancelledRides(cancelledRides)
                .totalDistanceKm(totalDistance)
                .totalEstimatedMinutes(totalEstimatedMinutes)
                .wheelchairRides((int) wheelchairRides)
                .stretcherRides((int) stretcherRides)
                .emergencyRides((int) emergencyRides)
                .averageRideDistance(totalRides > 0 ? totalDistance / totalRides : 0.0)
                .completionRate(totalRides > 0 ? (completedRides * 100.0) / totalRides : 0.0)
                .build();
    }

    private ErrorResponse createErrorResponse(String code, String message) {
        return new ErrorResponse(code, message, LocalDateTime.now());
    }

    private SuccessResponse createSuccessResponse(String message) {
        return new SuccessResponse(message, LocalDateTime.now());
    }

    // ========== RESPONSE CLASSES ==========

    public static class DriverDailySummary {
        private DriverDTO driver;
        private LocalDate date;
        private List<RideDetailDTO> rides;
        private DriverService.DriverWorkload workload;
        private DailySummaryStats summary;

        public static DriverDailySummaryBuilder builder() {
            return new DriverDailySummaryBuilder();
        }

        // Getters
        public DriverDTO getDriver() { return driver; }
        public LocalDate getDate() { return date; }
        public List<RideDetailDTO> getRides() { return rides; }
        public DriverService.DriverWorkload getWorkload() { return workload; }
        public DailySummaryStats getSummary() { return summary; }

        public static class DriverDailySummaryBuilder {
            private DriverDTO driver;
            private LocalDate date;
            private List<RideDetailDTO> rides;
            private DriverService.DriverWorkload workload;
            private DailySummaryStats summary;

            public DriverDailySummaryBuilder driver(DriverDTO driver) {
                this.driver = driver;
                return this;
            }

            public DriverDailySummaryBuilder date(LocalDate date) {
                this.date = date;
                return this;
            }

            public DriverDailySummaryBuilder rides(List<RideDetailDTO> rides) {
                this.rides = rides;
                return this;
            }

            public DriverDailySummaryBuilder workload(DriverService.DriverWorkload workload) {
                this.workload = workload;
                return this;
            }

            public DriverDailySummaryBuilder summary(DailySummaryStats summary) {
                this.summary = summary;
                return this;
            }

            public DriverDailySummary build() {
                DriverDailySummary summary = new DriverDailySummary();
                summary.driver = this.driver;
                summary.date = this.date;
                summary.rides = this.rides;
                summary.workload = this.workload;
                summary.summary = this.summary;
                return summary;
            }
        }
    }

    public static class DailySummaryStats {
        private int totalRides;
        private int completedRides;
        private int pendingRides;
        private int cancelledRides;
        private double totalDistanceKm;
        private int totalEstimatedMinutes;
        private int wheelchairRides;
        private int stretcherRides;
        private int emergencyRides;
        private double averageRideDistance;
        private double completionRate;

        public static DailySummaryStatsBuilder builder() {
            return new DailySummaryStatsBuilder();
        }

        // Getters
        public int getTotalRides() { return totalRides; }
        public int getCompletedRides() { return completedRides; }
        public int getPendingRides() { return pendingRides; }
        public int getCancelledRides() { return cancelledRides; }
        public double getTotalDistanceKm() { return totalDistanceKm; }
        public int getTotalEstimatedMinutes() { return totalEstimatedMinutes; }
        public int getWheelchairRides() { return wheelchairRides; }
        public int getStretcherRides() { return stretcherRides; }
        public int getEmergencyRides() { return emergencyRides; }
        public double getAverageRideDistance() { return averageRideDistance; }
        public double getCompletionRate() { return completionRate; }

        public static class DailySummaryStatsBuilder {
            private int totalRides;
            private int completedRides;
            private int pendingRides;
            private int cancelledRides;
            private double totalDistanceKm;
            private int totalEstimatedMinutes;
            private int wheelchairRides;
            private int stretcherRides;
            private int emergencyRides;
            private double averageRideDistance;
            private double completionRate;

            public DailySummaryStatsBuilder totalRides(int totalRides) {
                this.totalRides = totalRides;
                return this;
            }

            public DailySummaryStatsBuilder completedRides(int completedRides) {
                this.completedRides = completedRides;
                return this;
            }

            public DailySummaryStatsBuilder pendingRides(int pendingRides) {
                this.pendingRides = pendingRides;
                return this;
            }

            public DailySummaryStatsBuilder cancelledRides(int cancelledRides) {
                this.cancelledRides = cancelledRides;
                return this;
            }

            public DailySummaryStatsBuilder totalDistanceKm(double totalDistanceKm) {
                this.totalDistanceKm = totalDistanceKm;
                return this;
            }

            public DailySummaryStatsBuilder totalEstimatedMinutes(int totalEstimatedMinutes) {
                this.totalEstimatedMinutes = totalEstimatedMinutes;
                return this;
            }

            public DailySummaryStatsBuilder wheelchairRides(int wheelchairRides) {
                this.wheelchairRides = wheelchairRides;
                return this;
            }

            public DailySummaryStatsBuilder stretcherRides(int stretcherRides) {
                this.stretcherRides = stretcherRides;
                return this;
            }

            public DailySummaryStatsBuilder emergencyRides(int emergencyRides) {
                this.emergencyRides = emergencyRides;
                return this;
            }

            public DailySummaryStatsBuilder averageRideDistance(double averageRideDistance) {
                this.averageRideDistance = averageRideDistance;
                return this;
            }

            public DailySummaryStatsBuilder completionRate(double completionRate) {
                this.completionRate = completionRate;
                return this;
            }

            public DailySummaryStats build() {
                DailySummaryStats stats = new DailySummaryStats();
                stats.totalRides = this.totalRides;
                stats.completedRides = this.completedRides;
                stats.pendingRides = this.pendingRides;
                stats.cancelledRides = this.cancelledRides;
                stats.totalDistanceKm = this.totalDistanceKm;
                stats.totalEstimatedMinutes = this.totalEstimatedMinutes;
                stats.wheelchairRides = this.wheelchairRides;
                stats.stretcherRides = this.stretcherRides;
                stats.emergencyRides = this.emergencyRides;
                stats.averageRideDistance = this.averageRideDistance;
                stats.completionRate = this.completionRate;
                return stats;
            }
        }
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

        public String getMessage() { return message; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}