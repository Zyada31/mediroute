// 3. Updated UnifiedController to work with your existing services
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
import com.mediroute.service.ride.EnhancedMedicalTransportOptimizer;
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
    private final EnhancedMedicalTransportOptimizer medicalTransportOptimizer;

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
    }

    // ========== Optimization Endpoints ==========

    @Operation(summary = "Optimize rides for date", description = "Run optimization on all unassigned rides for a specific date")
    @PostMapping("/optimization/date/{date}")
    public ResponseEntity<String> optimizeForDate(
            @Parameter(description = "Date to optimize")
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info(" Running optimization for date: {}", date);

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        List<Ride> rides = rideRepository.findByPickupTimeBetweenWithDriversAndPatient(start, end).stream()
                .filter(ride -> ride.getPickupDriver() == null && ride.getDropoffDriver() == null)
                .toList();

        if (rides.isEmpty()) {
            return ResponseEntity.ok("No unassigned rides found for " + date);
        }

        var result = medicalTransportOptimizer.optimizeSchedule(rides);

        String message = String.format("Optimization complete for %s. Processed: %d rides, Success Rate: %.1f%%",
                date, rides.size(), result.getSuccessRate());

        return ResponseEntity.ok(message);
    }

    @Operation(summary = "Optimize specific rides", description = "Run optimization on a list of specific ride IDs")
    @PostMapping("/optimization/rides")
    public ResponseEntity<String> optimizeSpecificRides(@RequestBody List<Long> rideIds) {
        log.info(" Running optimization for {} specific rides", rideIds.size());

        List<Ride> rides = rideRepository.findAllById(rideIds);

        if (rides.isEmpty()) {
            return ResponseEntity.badRequest().body("No valid rides found for the provided IDs");
        }

        var result = medicalTransportOptimizer.optimizeSchedule(rides);

        String message = String.format("Optimization complete. Processed: %d rides, Success Rate: %.1f%%",
                rides.size(), result.getSuccessRate());

        return ResponseEntity.ok(message);
    }

    // ========== Data Retrieval Endpoints ==========

    @Operation(summary = "Get rides by date", description = "Retrieve all rides for a specific date")
    // Fix both endpoints to use proper JOIN FETCH queries
    @GetMapping("/rides/date/{date}")
    public ResponseEntity<List<RideDetailDTO>> getRidesByDate(
            @Parameter(description = "Date in YYYY-MM-DD format")
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("📋 Fetching rides for date: {}", date);
        List<RideDetailDTO> rides = rideService.findRidesByDate(date);
        return ResponseEntity.ok(rides);
    }

    @Operation(summary = "Get unassigned rides", description = "Retrieve unassigned rides for a specific date")
    @GetMapping("/rides/unassigned")
    public ResponseEntity<List<Ride>> getUnassignedRides(
            @Parameter(description = "Date to check for unassigned rides")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("📋 Fetching unassigned rides for date: {}", date);
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        List<Ride> rides = rideService.findUnassignedRides(date).stream()
                .filter(ride -> ride.getPickupDriver() == null && ride.getDropoffDriver() == null)
                .toList();
        return ResponseEntity.ok(rides);
    }

    @Operation(summary = "Get daily assignment summary", description = "Get summary of driver assignments for a date")
    @GetMapping("/assign/summary")
    public ResponseEntity<List<DriverRideSummary>> getDailySummary(
            @Parameter(description = "Date for summary")
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        return ResponseEntity.ok(summaryService.getSummaryForDate(start, end));
    }

//    @Operation(summary = "Get active drivers", description = "Retrieve all active drivers")
//    @GetMapping("/drivers/active")
//    public ResponseEntity<List<Driver>> getActiveDrivers() {
//        List<Driver> drivers = driverRepository.findByActiveTrue();
//        return ResponseEntity.ok(drivers);
//    }

    @Operation(summary = "Get qualified drivers", description = "Get drivers qualified for medical transport")
    @GetMapping("/drivers/qualified")
    public ResponseEntity<List<Driver>> getQualifiedDrivers() {
        List<Driver> drivers = driverRepository.findByActiveTrueAndIsTrainingCompleteTrue();
        return ResponseEntity.ok(drivers);
    }

    // ========== Audit and History Endpoints ==========

    @Operation(summary = "Get ride audit history", description = "Retrieve audit trail for a specific ride")
    @GetMapping("/rides/{rideId}/audit")
    public ResponseEntity<List<RideAudit>> getAuditLogsByRide(
            @Parameter(description = "Ride ID") @PathVariable Long rideId) {
        return ResponseEntity.ok(rideAuditRepository.findByRideIdOrderByChangedAtDesc(rideId));
    }

    @Operation(summary = "Get assignment audit logs", description = "Retrieve assignment audit records")
    @GetMapping("/assignment/audit")
    public ResponseEntity<List<AssignmentAudit>> getAuditLogs(
            @Parameter(description = "Start date for audit records")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date for audit records")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (startDate != null && endDate != null) {
            return ResponseEntity.ok(
                    assignmentAuditRepository.findByAssignmentDateBetweenOrderByAssignmentDateDesc(startDate, endDate));
        }

        return ResponseEntity.ok(
                assignmentAuditRepository.findAll(Sort.by(Sort.Direction.DESC, "assignmentTime")));
    }

    @Operation(summary = "Get optimization by batch ID", description = "Get optimization results by batch identifier")
    @GetMapping("/assignment/audit/{batchId}")
    public ResponseEntity<AssignmentAudit> getOptimizationByBatchId(
            @Parameter(description = "Optimization batch ID") @PathVariable String batchId) {

        return assignmentAuditRepository.findByBatchId(batchId)
                .map(audit -> ResponseEntity.ok(audit))
                .orElse(ResponseEntity.notFound().build());
    }

    // ========== Statistics Endpoints ==========

    @Operation(summary = "Get ride statistics", description = "Get statistical summary for rides on a specific date")
    @GetMapping("/statistics/rides")
    public ResponseEntity<RideStatisticsDTO> getRideStatisticsDTO(
            @Parameter(description = "Date for statistics")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        List<Ride> allRides = rideRepository.findByPickupTimeBetween(start, end);

        long totalRides = allRides.size();
        long assignedRides = allRides.stream()
                .filter(ride -> ride.getPickupDriver() != null || ride.getDropoffDriver() != null)
                .count();
        long emergencyRides = allRides.stream()
                .filter(ride -> ride.getPriority() == Priority.EMERGENCY)
                .count();
        long wheelchairRides = allRides.stream()
                .filter(ride -> "wheelchair_van".equals(ride.getRequiredVehicleType()))
                .count();
        long roundTripRides = allRides.stream()
                .filter(ride -> Boolean.TRUE.equals(ride.getIsRoundTrip()))
                .count();

        RideStatisticsDTO stats = RideStatisticsDTO.builder()
                .date(date)
                .totalRides((int) totalRides)
                .assignedRides((int) assignedRides)
                .unassignedRides((int) (totalRides - assignedRides))
                .emergencyRides((int) emergencyRides)
                .wheelchairRides((int) wheelchairRides)
                .roundTripRides((int) roundTripRides)
                .assignmentRate(totalRides > 0 ? (assignedRides * 100.0 / totalRides) : 0.0)
                .build();

        return ResponseEntity.ok(stats);
    }

    @Operation(summary = "Get driver statistics", description = "Get statistical summary of drivers")
    @GetMapping("/statistics/drivers")
    public ResponseEntity<DriverStatisticsDTO> getDriverStatistics() {
        List<Driver> activeDrivers = driverRepository.findByActiveTrue();

        long wheelchairAccessible = activeDrivers.stream()
                .filter(d -> Boolean.TRUE.equals(d.getWheelchairAccessible()))
                .count();
        long stretcherCapable = activeDrivers.stream()
                .filter(d -> Boolean.TRUE.equals(d.getStretcherCapable()))
                .count();
        long oxygenEquipped = activeDrivers.stream()
                .filter(d -> Boolean.TRUE.equals(d.getOxygenEquipped()))
                .count();

        DriverStatisticsDTO stats = DriverStatisticsDTO.builder()
                .totalActiveDrivers((long) activeDrivers.size())
                .wheelchairAccessibleCount((int) wheelchairAccessible)
                .stretcherCapableCount((int) stretcherCapable)
                .oxygenEquippedCount((int) oxygenEquipped)
                .build();

        return ResponseEntity.ok(stats);
    }

    // ========== Legacy Assignment Method ==========

    @Operation(summary = "Legacy assign today", description = "Legacy endpoint for today's ride assignment")
    @PostMapping("/assign/today")
    public ResponseEntity<String> assignToday() {
        LocalDateTime start = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusDays(1);

        List<Ride> rides = rideRepository.findByPickupTimeBetween(start, end).stream()
                .filter(ride -> ride.getPickupDriver() == null && ride.getDropoffDriver() == null)
                .toList();

        if (rides.isEmpty()) {
            return ResponseEntity.ok("No unassigned rides found for today");
        }

        var result = medicalTransportOptimizer.optimizeSchedule(rides);

        return ResponseEntity.ok(String.format("Assignment complete for today. Success Rate: %.1f%%",
                result.getSuccessRate()));
    }

//        private final RideEvidenceService service;
//
//        @PostMapping("/{rideId}/evidence")
//        public RideEvidenceDTO create(
//                @PathVariable Long rideId,
//                @RequestParam RideEvidenceEventType eventType,
//                @RequestParam(required = false) String note,
//                @RequestParam(required = false) Double lat,
//                @RequestParam(required = false) Double lng,
//                @RequestPart(required = false) MultipartFile signature,
//                @RequestPart(required = false) List<MultipartFile> photos
//        ) {
//            return service.create(rideId, eventType, note, lat, lng, signature, photos);
//        }
//
//        @GetMapping("/{rideId}/evidence")
//        public List<RideEvidenceDTO> list(@PathVariable Long rideId) {
//            return service.listByRide(rideId);
//        }

}