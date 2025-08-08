package com.mediroute.controller;

import com.mediroute.dto.DriverDTO;
import com.mediroute.dto.DriverDetailDTO;
import com.mediroute.dto.DriverCreateDTO;
import com.mediroute.dto.DriverUpdateDTO;
import com.mediroute.dto.DriverStatisticsDTO;
import com.mediroute.entity.Driver;
import com.mediroute.service.driver.DriverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
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

    // ========== CREATE/UPDATE OPERATIONS ==========

    @Operation(summary = "Create a new driver", description = "Create a new driver with medical transport capabilities")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Driver created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid driver data"),
            @ApiResponse(responseCode = "409", description = "Driver already exists")
    })
    @PostMapping
    public ResponseEntity<?> createDriver(@Valid @RequestBody DriverCreateDTO createRequest) {
        try {
            // Convert DTO to the format your service expects
            DriverDTO driverDTO = convertCreateDTOToDriverDTO(createRequest);
            Driver saved = driverService.createOrUpdateDriver(driverDTO, false);

            // Return DTO instead of entity to prevent lazy loading issues
            DriverDetailDTO response = DriverDetailDTO.fromEntity(saved);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalStateException e) {
            log.warn("Duplicate driver: {}", createRequest.getName());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(createErrorResponse("DUPLICATE_DRIVER", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.error("Invalid driver data for {}", createRequest.getName(), e);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("INVALID_DATA", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error creating driver: {}", createRequest.getName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to create driver"));
        }
    }

    @Operation(summary = "Update an existing driver", description = "Update driver information and capabilities")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateDriver(
            @Parameter(description = "Driver ID") @PathVariable Long id,
            @Valid @RequestBody DriverUpdateDTO updateRequest) {
        try {
            // Get existing driver
            var existingDriver = driverService.getDriverById(id);
            if (existingDriver.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // Convert and update
            DriverDTO driverDTO = convertUpdateDTOToDriverDTO(updateRequest, existingDriver.get());
            Driver updated = driverService.createOrUpdateDriver(driverDTO, true);

            DriverDetailDTO response = DriverDetailDTO.fromEntity(updated);
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
            @Valid @RequestBody List<DriverCreateDTO> createRequests) {

        log.info("Processing batch creation of {} drivers", createRequests.size());

        BatchDriverResponse response = new BatchDriverResponse();

        for (DriverCreateDTO createRequest : createRequests) {
            try {
                DriverDTO driverDTO = convertCreateDTOToDriverDTO(createRequest);
                Driver saved = driverService.createOrUpdateDriver(driverDTO, false);
                response.addSuccess(DriverDetailDTO.fromEntity(saved));

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
    public ResponseEntity<List<DriverDetailDTO>> getAllDrivers() {
        try {
            List<Driver> drivers = driverService.listAllDrivers();
            List<DriverDetailDTO> driverDTOs = drivers.stream()
                    .map(DriverDetailDTO::fromEntity)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(driverDTOs);
        } catch (Exception e) {
            log.error("Error retrieving drivers", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Get driver by ID", description = "Retrieve a specific driver by their ID")
    @GetMapping("/{id}")
    public ResponseEntity<DriverDetailDTO> getDriverById(
            @Parameter(description = "Driver ID") @PathVariable Long id) {
        try {
            return driverService.getDriverById(id)
                    .map(DriverDetailDTO::fromEntity)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error retrieving driver {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Get active drivers", description = "Retrieve only active drivers")
    @GetMapping("/active")
    public ResponseEntity<List<DriverDetailDTO>> getActiveDrivers() {
        try {
            List<Driver> activeDrivers = driverService.getQualifiedDrivers();
            List<DriverDetailDTO> driverDTOs = activeDrivers.stream()
                    .map(DriverDetailDTO::fromEntity)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(driverDTOs);
        } catch (Exception e) {
            log.error("Error retrieving active drivers", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Get drivers by capability", description = "Get drivers filtered by medical transport capabilities")
    @GetMapping("/by-capability")
    public ResponseEntity<List<DriverDetailDTO>> getDriversByCapability(
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

            List<DriverDetailDTO> driverDTOs = drivers.stream()
                    .map(DriverDetailDTO::fromEntity)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(driverDTOs);
        } catch (Exception e) {
            log.error("Error filtering drivers by capability", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== WORKLOAD AND AVAILABILITY ==========

    @Operation(summary = "Get driver workload", description = "Get driver workload for a specific date")
    @GetMapping("/{id}/workload")
    public ResponseEntity<?> getDriverWorkload(
            @Parameter(description = "Driver ID") @PathVariable Long id,
            @Parameter(description = "Date (YYYY-MM-DD)") @RequestParam LocalDate date) {
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

//    @Operation(summary = "Get all driver workloads", description = "Get workload summary for all drivers on a specific date")
//    @GetMapping("/workload")
//    public ResponseEntity<?> getAllDriverWorkloads(
//            @Parameter(description = "Date (YYYY-MM-DD)") @RequestParam LocalDate date) {
//        try {
//            var workloads = driverService.getAllDriverWorkloads(date);
//            return ResponseEntity.ok(workloads);
//        } catch (Exception e) {
//            log.error("Error getting all driver workloads for {}", date, e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//    }

    // ========== STATISTICS ==========
    @Operation(summary = "Get driver statistics", description = "Get comprehensive driver statistics")
    @GetMapping("/statistics")
    public ResponseEntity<DriverStatisticsDTO> getDriverStatistics() {
        try {
            var stats = driverService.getDriverStats();

            DriverStatisticsDTO statsDTO = DriverStatisticsDTO.builder()
                    .totalActiveDrivers((long) stats.getActiveDrivers())
                    .wheelchairAccessibleCount(stats.getWheelchairCapableDrivers())
                    .stretcherCapableCount(stats.getStretcherCapableDrivers())
                    .oxygenEquippedCount(stats.getOxygenEquippedDrivers())
                    .build();

            return ResponseEntity.ok(statsDTO);
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
    private DriverDTO convertCreateDTOToDriverDTO(DriverCreateDTO createDTO) {
        DriverDTO driverDTO = new DriverDTO(
                createDTO.getName(),
                createDTO.getEmail(),
                createDTO.getPhone(),
                createDTO.getVehicleType() != null ? createDTO.getVehicleType().name() : "SEDAN",
                new java.util.HashMap<>(), // skills
                null, // shiftStart - will be converted below
                null, // shiftEnd - will be converted below
                createDTO.getBaseLocation(),
                createDTO.getMaxDailyRides(),
                createDTO.getWheelchairAccessible(),
                createDTO.getStretcherCapable(),
                createDTO.getOxygenEquipped(),
                createDTO.getIsTrainingComplete(),
                createDTO.getIsTrainingComplete(),
                new java.util.ArrayList<>() // certifications
        );

        // Convert LocalTime to LocalDateTime for compatibility
        if (createDTO.getShiftStart() != null) {
            driverDTO.setShiftStart(java.time.LocalDate.now().atTime(createDTO.getShiftStart()));
        }
        if (createDTO.getShiftEnd() != null) {
            driverDTO.setShiftEnd(java.time.LocalDate.now().atTime(createDTO.getShiftEnd()));
        }

        return driverDTO;
    }

    private DriverDTO convertUpdateDTOToDriverDTO(DriverUpdateDTO updateDTO, Driver existingDriver) {
        DriverDTO driverDTO = new DriverDTO(
                updateDTO.getName() != null ? updateDTO.getName() : existingDriver.getName(),
                updateDTO.getEmail() != null ? updateDTO.getEmail() : existingDriver.getEmail(),
                updateDTO.getPhone() != null ? updateDTO.getPhone() : existingDriver.getPhone(),
                updateDTO.getVehicleType() != null ? updateDTO.getVehicleType().name() : existingDriver.getVehicleType().name(),
                existingDriver.getSkills(),
                null, // will set below
                null, // will set below
                existingDriver.getBaseLocation(),
                existingDriver.getMaxDailyRides(),
                updateDTO.getWheelchairAccessible() != null ? updateDTO.getWheelchairAccessible() : existingDriver.getWheelchairAccessible(),
                updateDTO.getStretcherCapable() != null ? updateDTO.getStretcherCapable() : existingDriver.getStretcherCapable(),
                updateDTO.getOxygenEquipped() != null ? updateDTO.getOxygenEquipped() : existingDriver.getOxygenEquipped(),
                updateDTO.getIsTrainingComplete() != null ? updateDTO.getIsTrainingComplete() : existingDriver.getIsTrainingComplete(),
                updateDTO.getIsTrainingComplete() != null ? updateDTO.getIsTrainingComplete() : existingDriver.getIsTrainingComplete(),
                existingDriver.getCertifications()
        );

        // Convert LocalTime to LocalDateTime for compatibility
        if (updateDTO.getShiftStart() != null) {
            driverDTO.setShiftStart(java.time.LocalDate.now().atTime(updateDTO.getShiftStart()));
        } else if (existingDriver.getShiftStart() != null) {
            driverDTO.setShiftStart(java.time.LocalDate.now().atTime(existingDriver.getShiftStart()));
        }

        if (updateDTO.getShiftEnd() != null) {
            driverDTO.setShiftEnd(java.time.LocalDate.now().atTime(updateDTO.getShiftEnd()));
        } else if (existingDriver.getShiftEnd() != null) {
            driverDTO.setShiftEnd(java.time.LocalDate.now().atTime(existingDriver.getShiftEnd()));
        }

        return driverDTO;
    }

    private ErrorResponse createErrorResponse(String code, String message) {
        return new ErrorResponse(code, message, java.time.LocalDateTime.now());
    }

    private SuccessResponse createSuccessResponse(String message) {
        return new SuccessResponse(message, java.time.LocalDateTime.now());
    }

    // ========== RESPONSE CLASSES ==========
    public static class BatchDriverResponse {
        private List<DriverDetailDTO> successful = new ArrayList<>();
        private List<DriverError> errors = new ArrayList<>();

        public void addSuccess(DriverDetailDTO driver) {
            successful.add(driver);
        }

        public void addError(String driverName, String errorCode, String message) {
            errors.add(new DriverError(driverName, errorCode, message));
        }

        // Getters
        public List<DriverDetailDTO> getSuccessful() { return successful; }
        public List<DriverError> getErrors() { return errors; }
        public int getSuccessCount() { return successful.size(); }
        public int getErrorCount() { return errors.size(); }
    }

    public static class DriverError {
        private String driverName;
        private String errorCode;
        private String message;
        private java.time.LocalDateTime timestamp = java.time.LocalDateTime.now();

        public DriverError(String driverName, String errorCode, String message) {
            this.driverName = driverName;
            this.errorCode = errorCode;
            this.message = message;
        }

        // Getters
        public String getDriverName() { return driverName; }
        public String getErrorCode() { return errorCode; }
        public String getMessage() { return message; }
        public java.time.LocalDateTime getTimestamp() { return timestamp; }
    }

    public static class ErrorResponse {
        private String errorCode;
        private String message;
        private java.time.LocalDateTime timestamp;

        public ErrorResponse(String errorCode, String message, java.time.LocalDateTime timestamp) {
            this.errorCode = errorCode;
            this.message = message;
            this.timestamp = timestamp;
        }

        // Getters
        public String getErrorCode() { return errorCode; }
        public String getMessage() { return message; }
        public java.time.LocalDateTime getTimestamp() { return timestamp; }
    }

    public static class SuccessResponse {
        private String message;
        private java.time.LocalDateTime timestamp;

        public SuccessResponse(String message, java.time.LocalDateTime timestamp) {
            this.message = message;
            this.timestamp = timestamp;
        }

        // Getters
        public String getMessage() { return message; }
        public java.time.LocalDateTime getTimestamp() { return timestamp; }
    }
}