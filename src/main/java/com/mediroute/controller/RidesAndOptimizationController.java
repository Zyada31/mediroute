package com.mediroute.controller;

import com.mediroute.dto.*;
import com.mediroute.entity.AssignmentAudit;
import com.mediroute.entity.Driver;
import com.mediroute.entity.Ride;
import com.mediroute.entity.RideAudit;
import com.mediroute.repository.AssignmentAuditRepository;
import com.mediroute.repository.DriverRepository;
import com.mediroute.repository.RideAuditRepository;
import com.mediroute.service.assigment.AssignmentSummaryService;
import com.mediroute.service.driver.DriverService;
import com.mediroute.service.parser.ExcelParserService;
import com.mediroute.service.ride.OptimizationIntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@CrossOrigin(origins = {"http://localhost:3000", "https://app.mediroute.com"})
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
/**
 * Rides & Optimization Controller
 * Structure:
 * - File upload & parse endpoints
 * - Optimization submit endpoints
 * - Ride listing/statistics endpoints
 * - Ride status updates (migrated from RideStatusController)
 */
@Tag(name = "Rides", description = "Rides ingestion, listing, statistics, and optimization")
public class RidesAndOptimizationController {

    private final com.mediroute.service.ride.RideService rideService;
    private final ExcelParserService excelParserService;
    private final DriverRepository driverRepository;
    private final RideAuditRepository rideAuditRepository;
    private final AssignmentAuditRepository assignmentAuditRepository;
    private final DriverService driverService;
    private final AssignmentSummaryService summaryService;
    private final OptimizationIntegrationService optimizationService;

    @Operation(summary = "Upload Excel/CSV file", description = "Parse and import rides from Excel or CSV file")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File processed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid file format"),
            @ApiResponse(responseCode = "500", description = "Processing error")
    })
    @PostMapping("/rides/upload")
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN','DISPATCHER')")
    public ResponseEntity<ParseResult> uploadExcel(
            @Parameter(description = "Excel or CSV file containing ride data")
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Assignment date for the rides")
            @RequestParam(name = "assignmentDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate assignmentDate) throws IOException {

        log.info("Received file upload: {}", file.getOriginalFilename());

        if (assignmentDate == null) {
            assignmentDate = LocalDate.now().plusDays(1);
            log.info("No assignmentDate provided, defaulting to {}", assignmentDate);
        }

        ParseResult result = excelParserService.parseExcelWithMedicalFeatures(file, assignmentDate, false);
        log.info("Processed {} rides from file", result.getSuccessfulRows());

        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Upload and optimize", description = "Parse file and immediately run optimization")
    @PostMapping("/rides/upload-and-optimize")
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN','DISPATCHER')")
    public ResponseEntity<ParseResult> uploadAndOptimize(
            @Parameter(description = "Excel or CSV file containing ride data")
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Assignment date for the rides")
            @RequestParam(name = "assignmentDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate assignmentDate) throws IOException {

        log.info("Received file upload with optimization: {}", file.getOriginalFilename());

        if (assignmentDate == null) {
            assignmentDate = LocalDate.now().plusDays(1);
        }

        ParseResult result = excelParserService.parseExcelWithMedicalFeatures(file, assignmentDate, true);
        log.info("Processed and optimized {} rides from file", result.getSuccessfulRows());

        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Optimize rides for date", description = "Run optimization on all unassigned rides for a specific date")
    @PostMapping("/optimization/date/{date}")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public ResponseEntity<OptimizationResult> optimizeForDate(
            @Parameter(description = "Date to optimize")
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("🔧 Running optimization for date: {}", date);

        try {
            OptimizationResult result = optimizationService.optimizeRidesForDate(date);

            String message = String.format("Optimization complete for %s. Success Rate: %.1f%%",
                    date, result.getSuccessRate());
            log.info(message);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Optimization failed for date {}: {}", date, e.getMessage(), e);

            OptimizationResult errorResult = OptimizationResult.builder()
                    .optimizationRan(false)
                    .optimizationError(e.getMessage())
                    .totalRides(0)
                    .successRate(0.0)
                    .build();

            return ResponseEntity.ok(errorResult);
        }
    }

    @Operation(summary = "Optimize specific rides", description = "Run optimization on a list of specific ride IDs")
    @PostMapping("/optimization/rides")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public ResponseEntity<OptimizationResult> optimizeSpecificRides(@RequestBody List<Long> rideIds) {
        log.info("🔧 Running optimization for {} specific rides", rideIds.size());

        try {
            OptimizationResult result = optimizationService.optimizeSpecificRides(rideIds);

            String message = String.format("Optimization complete. Success Rate: %.1f%%", result.getSuccessRate());
            log.info(message);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Specific ride optimization failed: {}", e.getMessage(), e);

            OptimizationResult errorResult = OptimizationResult.builder()
                    .optimizationRan(false)
                    .optimizationError(e.getMessage())
                    .totalRides(rideIds.size())
                    .successRate(0.0)
                    .build();

            return ResponseEntity.ok(errorResult);
        }
    }

    @Operation(summary = "Optimize rides in time range", description = "Run optimization on rides within a specific time range")
    @PostMapping("/optimization/timerange")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public ResponseEntity<OptimizationResult> optimizeTimeRange(
            @Parameter(description = "Start time") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "End time") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        log.info("🔧 Running optimization for time range: {} to {}", startTime, endTime);

        try {
            OptimizationResult result = optimizationService.optimizeRidesInTimeRange(startTime, endTime);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Time range optimization failed: {}", e.getMessage(), e);

            OptimizationResult errorResult = OptimizationResult.builder()
                    .optimizationRan(false)
                    .optimizationError(e.getMessage())
                    .successRate(0.0)
                    .build();

            return ResponseEntity.ok(errorResult);
        }
    }

    @Operation(
            summary = "Get rides by date",
            description = "Retrieve all rides for a specific date (paginated). Results are scoped to the caller's organization.")
    @GetMapping("/rides/date/{date}")
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN','DISPATCHER')")
    public ResponseEntity<com.mediroute.dto.PageResponse<RideDetailDTO>> getRidesByDate(
            @Parameter(description = "Date in YYYY-MM-DD format")
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "25")
            @RequestParam(defaultValue = "25") int size,
            @Parameter(description = "Sort directive: field,dir (asc|desc)", example = "pickupTime,asc")
            @RequestParam(defaultValue = "pickupTime,asc") String sort) {

        log.info("Fetching rides for date: {}", date);

        try {
            String[] sortParts = sort.split(",");
            Sort s = (sortParts.length == 2 && sortParts[1].equalsIgnoreCase("desc"))
                    ? Sort.by(sortParts[0]).descending()
                    : Sort.by(sortParts[0]).ascending();
            Pageable pageable = PageRequest.of(page, size, s);

            Page<Ride> pg = rideService.findRidesByDatePaged(date, pageable);
            List<RideDetailDTO> items = pg.getContent().stream().map(RideDetailDTO::fromEntity).toList();
            var resp = new com.mediroute.dto.PageResponse<>(items, pg.getNumber(), pg.getSize(), pg.getTotalElements(), pg.getTotalPages(), sort);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            log.error("Error fetching rides for date {}: {}", date, e.getMessage(), e);
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Failed to fetch rides");
        }
    }

    @Operation(
            summary = "Get unassigned rides",
            description = "Retrieve unassigned rides for a specific date (paginated). Results are scoped to the caller's organization.")
    @GetMapping("/rides/unassigned")
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN','DISPATCHER')")
    public ResponseEntity<com.mediroute.dto.PageResponse<RideDetailDTO>> getUnassignedRides(
            @Parameter(description = "Date to check for unassigned rides")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "25")
            @RequestParam(defaultValue = "25") int size,
            @Parameter(description = "Sort directive: field,dir (asc|desc)", example = "priority,desc")
            @RequestParam(defaultValue = "priority,desc") String sort) {

        log.info("Fetching unassigned rides for date: {}", date);

        try {
            String[] sortParts = sort.split(",");
            Sort s = (sortParts.length == 2 && sortParts[1].equalsIgnoreCase("desc"))
                    ? Sort.by(sortParts[0]).descending()
                    : Sort.by(sortParts[0]).ascending();
            Pageable pageable = PageRequest.of(page, size, s);

            Page<Ride> pg = rideService.findUnassignedRidesPaged(date, pageable);
            List<RideDetailDTO> items = pg.getContent().stream().map(RideDetailDTO::fromEntity).toList();
            var resp = new com.mediroute.dto.PageResponse<>(items, pg.getNumber(), pg.getSize(), pg.getTotalElements(), pg.getTotalPages(), sort);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            log.error("Error fetching unassigned rides for date {}: {}", date, e.getMessage(), e);
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Failed to fetch unassigned rides");
        }
    }

    @Operation(summary = "Get daily assignment summary", description = "Get summary of driver assignments for a date")
    @GetMapping("/assign/summary")
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN','DISPATCHER')")
    public ResponseEntity<List<DriverRideSummary>> getDailySummary(
            @Parameter(description = "Date for summary")
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        try {
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = start.plusDays(1);
            return ResponseEntity.ok(summaryService.getSummaryForDate(start, end));
        } catch (Exception e) {
            log.error("Error getting daily summary for {}: {}", date, e.getMessage(), e);
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Failed to get daily summary");
        }
    }

    @Operation(summary = "Get qualified drivers", description = "Get drivers qualified for medical transport. Results are scoped to the caller's organization.")
    @GetMapping("/drivers/qualified")
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN','DISPATCHER')")
    public ResponseEntity<List<Driver>> getQualifiedDrivers() {
        try {
            List<Driver> drivers = driverRepository.findByOrgIdAndActiveTrueAndIsTrainingCompleteTrue(com.mediroute.config.SecurityBeans.currentOrgId());
            return ResponseEntity.ok(drivers);
        } catch (Exception e) {
            log.error("Error getting qualified drivers: {}", e.getMessage(), e);
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Failed to get qualified drivers");
        }
    }

    @Operation(summary = "Get ride audit history", description = "Retrieve audit trail for a specific ride. Results are scoped to the caller's organization.")
    @GetMapping("/rides/{rideId}/audit")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public ResponseEntity<List<RideAudit>> getAuditLogsByRide(
            @Parameter(description = "Ride ID") @PathVariable Long rideId) {
        try {
            return ResponseEntity.ok(rideAuditRepository.findByRideIdOrderByChangedAtDesc(rideId));
        } catch (Exception e) {
            log.error("Error getting audit logs for ride {}: {}", rideId, e.getMessage(), e);
            return ResponseEntity.ok(List.of());
        }
    }

    @Operation(summary = "Get assignment audit logs", description = "Retrieve assignment audit records. Results are scoped to the caller's organization.")
    @GetMapping("/assignment/audit")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public ResponseEntity<List<AssignmentAudit>> getAuditLogs(
            @Parameter(description = "Start date for audit records")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date for audit records")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            if (startDate != null && endDate != null) {
                return ResponseEntity.ok(
                        assignmentAuditRepository.findByOrgIdAndAssignmentDateBetweenOrderByAssignmentDateDesc(com.mediroute.config.SecurityBeans.currentOrgId(), startDate, endDate));
            }

            return ResponseEntity.ok(
                    assignmentAuditRepository.findAllOrderByAssignmentTimeDesc(com.mediroute.config.SecurityBeans.currentOrgId()));
        } catch (Exception e) {
            log.error("Error getting assignment audit logs: {}", e.getMessage(), e);
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Failed to get assignment audit logs");
        }
    }

    @Operation(summary = "Get optimization by batch ID", description = "Get optimization results by batch identifier. Results are scoped to the caller's organization.")
    @GetMapping("/assignment/audit/{batchId}")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public ResponseEntity<AssignmentAudit> getOptimizationByBatchId(
            @Parameter(description = "Optimization batch ID") @PathVariable String batchId) {

        try {
            return assignmentAuditRepository.findByOrgIdAndBatchId(com.mediroute.config.SecurityBeans.currentOrgId(), batchId)
                    .map(audit -> ResponseEntity.ok(audit))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error getting optimization by batch ID {}: {}", batchId, e.getMessage(), e);
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Failed to get optimization batch");
        }
    }

    @Operation(summary = "Get ride statistics", description = "Get statistical summary for rides on a specific date. Results are scoped to the caller's organization.")
    @GetMapping("/statistics/rides")
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN','DISPATCHER')")
    public ResponseEntity<RideStatisticsDTO> getRideStatisticsDTO(
            @Parameter(description = "Date for statistics")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        try {
            RideStatisticsDTO stats = rideService.getRideStatistics(date);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting ride statistics for {}: {}", date, e.getMessage(), e);
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Failed to get ride statistics");
        }
    }

    @Operation(summary = "Get driver statistics", description = "Get statistical summary of drivers. Results are scoped to the caller's organization.")
    @GetMapping("/statistics/drivers")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public ResponseEntity<DriverStatisticsDTO> getDriverStatistics() {
        try {
            DriverStatisticsDTO stats = driverService.getDriverStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting driver statistics: {}", e.getMessage(), e);
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Failed to get driver statistics");
        }
    }

    @Operation(summary = "Get optimization statistics", description = "Get optimization performance statistics. Results are scoped to the caller's organization.")
    @GetMapping("/statistics/optimization")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public ResponseEntity<OptimizationIntegrationService.OptimizationStats> getOptimizationStatistics(
            @Parameter(description = "Date for optimization statistics")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        try {
            OptimizationIntegrationService.OptimizationStats stats = optimizationService.getOptimizationStats(date);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting optimization statistics for {}: {}", date, e.getMessage(), e);
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Failed to get optimization statistics");
        }
    }

    /**
     * Update the status of a ride. Drivers may update their own assigned rides; dispatch/admin can update any.
     */
    public record StatusUpdate(String status, String notes) {}

    @PostMapping("/rides/{id}/status")
    @PreAuthorize("hasAnyRole('DRIVER','DISPATCHER','ADMIN')")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody StatusUpdate update, Authentication auth) {
        String roleString = auth.getAuthorities().toString();
        Long driverId = null;
        Object details = auth.getDetails();
        if (details instanceof Map<?,?> map && map.get("driverId") != null) {
            try { driverId = Long.valueOf(map.get("driverId").toString()); } catch (Exception ignored) {}
        }
        try {
            com.mediroute.dto.RideStatus newStatus = com.mediroute.dto.RideStatus.valueOf(update.status());
            com.mediroute.entity.Ride updated = rideService.updateStatus(id, newStatus, driverId, roleString, update.notes());
            return ResponseEntity.ok(Map.of("id", updated.getId(), "status", updated.getStatus()));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (com.mediroute.exceptions.RideAssignmentException e) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update ride status");
        }
    }
}


