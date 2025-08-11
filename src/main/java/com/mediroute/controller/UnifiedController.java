// Updated UnifiedController with proper optimization integration
package com.mediroute.controller;

import com.mediroute.dto.*;
import com.mediroute.entity.AssignmentAudit;
import com.mediroute.entity.Driver;
import com.mediroute.entity.Ride;
import com.mediroute.entity.RideAudit;
import com.mediroute.repository.AssignmentAuditRepository;
import com.mediroute.repository.DriverRepository;
import com.mediroute.repository.RideAuditRepository;
import com.mediroute.repository.RideRepository;
import com.mediroute.service.assigment.AssignmentSummaryService;
import com.mediroute.service.driver.DriverService;
import com.mediroute.service.parser.ExcelParserService;
import com.mediroute.service.ride.RideService;
import com.mediroute.service.ride.OptimizationIntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@CrossOrigin(origins = {"http://localhost:3000", "https://app.mediroute.com"})
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "MediRoute API", description = "Medical transport management operations")
public class UnifiedController {

    private final RideService rideService;
    private final ExcelParserService excelParserService;
    private final RideRepository rideRepository;
    private final DriverRepository driverRepository;
    private final RideAuditRepository rideAuditRepository;
    private final AssignmentAuditRepository assignmentAuditRepository;
    private final DriverService driverService;
    private final AssignmentSummaryService summaryService;
    private final OptimizationIntegrationService optimizationService; // Use integration service

    // ========== Enhanced File Upload Endpoints ==========

    @Operation(summary = "Upload Excel/CSV file", description = "Parse and import rides from Excel or CSV file")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File processed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid file format"),
            @ApiResponse(responseCode = "500", description = "Processing error")
    })
    @PostMapping("/rides/upload")
    public ResponseEntity<ParseResult> uploadExcel(
            @Parameter(description = "Excel or CSV file containing ride data")
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Assignment date for the rides")
            @RequestParam(name = "assignmentDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate assignmentDate) throws IOException {

        log.info("📥 Received file upload: {}", file.getOriginalFilename());

        if (assignmentDate == null) {
            assignmentDate = LocalDate.now().plusDays(1);
            log.info("📅 No assignmentDate provided, defaulting to {}", assignmentDate);
        }

        ParseResult result = excelParserService.parseExcelWithMedicalFeatures(file, assignmentDate, false);
        log.info("Processed {} rides from file", result.getSuccessfulRows());

        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Upload and optimize", description = "Parse file and immediately run optimization")
    @PostMapping("/rides/upload-and-optimize")
    public ResponseEntity<ParseResult> uploadAndOptimize(
            @Parameter(description = "Excel or CSV file containing ride data")
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Assignment date for the rides")
            @RequestParam(name = "assignmentDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate assignmentDate) throws IOException {

        log.info("📥 Received file upload with optimization: {}", file.getOriginalFilename());

        if (assignmentDate == null) {
            assignmentDate = LocalDate.now().plusDays(1);
        }

        ParseResult result = excelParserService.parseExcelWithMedicalFeatures(file, assignmentDate, true);
        log.info("Processed and optimized {} rides from file", result.getSuccessfulRows());

        return ResponseEntity.ok(result);
    }

    // ========== Enhanced Optimization Endpoints ==========

    @Operation(summary = "Optimize rides for date", description = "Run optimization on all unassigned rides for a specific date")
    @PostMapping("/optimization/date/{date}")
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
            log.error("❌ Optimization failed for date {}: {}", date, e.getMessage(), e);

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
    public ResponseEntity<OptimizationResult> optimizeSpecificRides(@RequestBody List<Long> rideIds) {
        log.info("🔧 Running optimization for {} specific rides", rideIds.size());

        try {
            OptimizationResult result = optimizationService.optimizeSpecificRides(rideIds);

            String message = String.format("Optimization complete. Success Rate: %.1f%%", result.getSuccessRate());
            log.info(message);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Specific ride optimization failed: {}", e.getMessage(), e);

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
    public ResponseEntity<OptimizationResult> optimizeTimeRange(
            @Parameter(description = "Start time") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "End time") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        log.info("🔧 Running optimization for time range: {} to {}", startTime, endTime);

        try {
            OptimizationResult result = optimizationService.optimizeRidesInTimeRange(startTime, endTime);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Time range optimization failed: {}", e.getMessage(), e);

            OptimizationResult errorResult = OptimizationResult.builder()
                    .optimizationRan(false)
                    .optimizationError(e.getMessage())
                    .successRate(0.0)
                    .build();

            return ResponseEntity.ok(errorResult);
        }
    }

    // ========== Data Retrieval Endpoints ==========

    @Operation(summary = "Get rides by date", description = "Retrieve all rides for a specific date")
    @GetMapping("/rides/date/{date}")
    public ResponseEntity<List<RideDetailDTO>> getRidesByDate(
            @Parameter(description = "Date in YYYY-MM-DD format")
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("📋 Fetching rides for date: {}", date);

        try {
            List<RideDetailDTO> rides = rideService.findRidesByDate(date);
            return ResponseEntity.ok(rides);
        } catch (Exception e) {
            log.error("Error fetching rides for date {}: {}", date, e.getMessage(), e);
            return ResponseEntity.ok(List.of()); // Return empty list instead of error
        }
    }

    @Operation(summary = "Get unassigned rides", description = "Retrieve unassigned rides for a specific date")
    @GetMapping("/rides/unassigned")
    public ResponseEntity<List<RideDetailDTO>> getUnassignedRides(
            @Parameter(description = "Date to check for unassigned rides")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("📋 Fetching unassigned rides for date: {}", date);

        try {
            List<RideDetailDTO> rides = rideService.findUnassignedRidesAsDTO(date);
            return ResponseEntity.ok(rides);
        } catch (Exception e) {
            log.error("Error fetching unassigned rides for date {}: {}", date, e.getMessage(), e);
            return ResponseEntity.ok(List.of());
        }
    }

    @Operation(summary = "Get daily assignment summary", description = "Get summary of driver assignments for a date")
    @GetMapping("/assign/summary")
    public ResponseEntity<List<DriverRideSummary>> getDailySummary(
            @Parameter(description = "Date for summary")
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        try {
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = start.plusDays(1);
            return ResponseEntity.ok(summaryService.getSummaryForDate(start, end));
        } catch (Exception e) {
            log.error("Error getting daily summary for {}: {}", date, e.getMessage(), e);
            return ResponseEntity.ok(List.of());
        }
    }

    @Operation(summary = "Get qualified drivers", description = "Get drivers qualified for medical transport")
    @GetMapping("/drivers/qualified")
    public ResponseEntity<List<Driver>> getQualifiedDrivers() {
        try {
            List<Driver> drivers = driverRepository.findByActiveTrueAndIsTrainingCompleteTrue();
            return ResponseEntity.ok(drivers);
        } catch (Exception e) {
            log.error("Error getting qualified drivers: {}", e.getMessage(), e);
            return ResponseEntity.ok(List.of());
        }
    }

    // ========== Audit and History Endpoints ==========

    @Operation(summary = "Get ride audit history", description = "Retrieve audit trail for a specific ride")
    @GetMapping("/rides/{rideId}/audit")
    public ResponseEntity<List<RideAudit>> getAuditLogsByRide(
            @Parameter(description = "Ride ID") @PathVariable Long rideId) {
        try {
            return ResponseEntity.ok(rideAuditRepository.findByRideIdOrderByChangedAtDesc(rideId));
        } catch (Exception e) {
            log.error("Error getting audit logs for ride {}: {}", rideId, e.getMessage(), e);
            return ResponseEntity.ok(List.of());
        }
    }

    @Operation(summary = "Get assignment audit logs", description = "Retrieve assignment audit records")
    @GetMapping("/assignment/audit")
    public ResponseEntity<List<AssignmentAudit>> getAuditLogs(
            @Parameter(description = "Start date for audit records")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date for audit records")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            if (startDate != null && endDate != null) {
                return ResponseEntity.ok(
                        assignmentAuditRepository.findByAssignmentDateBetweenOrderByAssignmentDateDesc(startDate, endDate));
            }

            return ResponseEntity.ok(
                    assignmentAuditRepository.findAll(Sort.by(Sort.Direction.DESC, "assignmentTime")));
        } catch (Exception e) {
            log.error("Error getting assignment audit logs: {}", e.getMessage(), e);
            return ResponseEntity.ok(List.of());
        }
    }

    @Operation(summary = "Get optimization by batch ID", description = "Get optimization results by batch identifier")
    @GetMapping("/assignment/audit/{batchId}")
    public ResponseEntity<AssignmentAudit> getOptimizationByBatchId(
            @Parameter(description = "Optimization batch ID") @PathVariable String batchId) {

        try {
            return assignmentAuditRepository.findByBatchId(batchId)
                    .map(audit -> ResponseEntity.ok(audit))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error getting optimization by batch ID {}: {}", batchId, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }

    // ========== Statistics Endpoints ==========

    @Operation(summary = "Get ride statistics", description = "Get statistical summary for rides on a specific date")
    @GetMapping("/statistics/rides")
    public ResponseEntity<RideStatisticsDTO> getRideStatisticsDTO(
            @Parameter(description = "Date for statistics")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        try {
            RideStatisticsDTO stats = rideService.getRideStatistics(date);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting ride statistics for {}: {}", date, e.getMessage(), e);

            // Return empty stats instead of error
            RideStatisticsDTO emptyStats = RideStatisticsDTO.builder()
                    .date(date)
                    .totalRides(0)
                    .assignedRides(0)
                    .unassignedRides(0)
                    .assignmentRate(0.0)
                    .build();
            return ResponseEntity.ok(emptyStats);
        }
    }

    @Operation(summary = "Get driver statistics", description = "Get statistical summary of drivers")
    @GetMapping("/statistics/drivers")
    public ResponseEntity<DriverStatisticsDTO> getDriverStatistics() {
        try {
            DriverStatisticsDTO stats = driverService.getDriverStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting driver statistics: {}", e.getMessage(), e);

            // Return empty stats
            DriverStatisticsDTO emptyStats = DriverStatisticsDTO.builder()
                    .totalActiveDrivers(0L)
                    .wheelchairAccessibleCount(0)
                    .stretcherCapableCount(0)
                    .oxygenEquippedCount(0)
                    .build();
            return ResponseEntity.ok(emptyStats);
        }
    }

    @Operation(summary = "Get optimization statistics", description = "Get optimization performance statistics")
    @GetMapping("/statistics/optimization")
    public ResponseEntity<OptimizationIntegrationService.OptimizationStats> getOptimizationStatistics(
            @Parameter(description = "Date for optimization statistics")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        try {
            OptimizationIntegrationService.OptimizationStats stats = optimizationService.getOptimizationStats(date);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting optimization statistics for {}: {}", date, e.getMessage(), e);

            // Return empty stats
            OptimizationIntegrationService.OptimizationStats emptyStats =
                    OptimizationIntegrationService.OptimizationStats.builder()
                            .date(date)
                            .totalRides(0)
                            .assignedRides(0)
                            .unassignedRides(0)
                            .assignmentRate(0.0)
                            .build();
            return ResponseEntity.ok(emptyStats);
        }
    }

    // ========== Legacy Assignment Method ==========

    @Operation(summary = "Legacy assign today", description = "Legacy endpoint for today's ride assignment")
    @PostMapping("/assign/today")
    public ResponseEntity<String> assignToday() {
        try {
            LocalDate today = LocalDate.now();
            OptimizationResult result = optimizationService.optimizeRidesForDate(today);

            return ResponseEntity.ok(String.format("Assignment complete for today. Success Rate: %.1f%%",
                    result.getSuccessRate()));
        } catch (Exception e) {
            log.error("Error in legacy assign today: {}", e.getMessage(), e);
            return ResponseEntity.ok("Assignment failed: " + e.getMessage());
        }
    }

    // ========== Legacy Endpoints (Backward Compatibility) ==========

    @Operation(summary = "Legacy upload endpoint", description = "Legacy endpoint for backward compatibility")
    @PostMapping("/schedule/auto")
    public ResponseEntity<String> autoSchedule(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "assignmentDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate assignmentDate) throws IOException {

        if (assignmentDate == null) {
            assignmentDate = LocalDate.now(ZoneId.of("America/Denver")).plusDays(1);
        }

        log.info("📆 Legacy scheduling for assignmentDate={}", assignmentDate);

        try {
            ParseResult result = excelParserService.parseExcelWithMedicalFeatures(file, assignmentDate, true);

            if (result.getRides().isEmpty()) {
                return ResponseEntity.badRequest().body("No rides parsed from file for " + assignmentDate);
            }

            String message = String.format("Scheduling complete for %s. Rides: %d, Success Rate: %.1f%%",
                    assignmentDate, result.getSuccessfulRows(), result.getSuccessRate());

            if (result.getOptimizationRan() && result.getOptimizationResult() != null) {
                message += String.format(", Optimization: %.1f%% assigned",
                        result.getOptimizationResult().getSuccessRate());
            }

            return ResponseEntity.ok(message);

        } catch (Exception e) {
            log.error("Legacy scheduling failed: {}", e.getMessage(), e);
            return ResponseEntity.ok("Scheduling failed: " + e.getMessage());
        }
    }
}