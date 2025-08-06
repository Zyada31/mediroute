package com.mediroute.controller;

import com.mediroute.dto.DriverRideSummary;
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
import com.mediroute.service.ride.RideService;
import lombok.RequiredArgsConstructor;
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
import java.util.logging.Logger;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UnifiedController {

    Logger log = Logger.getLogger(UnifiedController.class.getName());

    private final RideService rideService;
    private final RideRepository rideRepository;
    private final DriverRepository driverRepository;
    private final RideAuditRepository rideAuditRepository;
    private final AssignmentAuditRepository assignmentAuditRepository;
    private final DriverService assignmentService;
    private final AssignmentSummaryService summaryService;

    // Rides
    @PostMapping("/rides/upload")
    public ResponseEntity<String> uploadExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "assignmentDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate assignmentDate
    ) throws IOException {
        log.info("📥 Received Excel upload with size: {}");

        if (assignmentDate == null) {
            assignmentDate = LocalDate.now().plusDays(1);
            log.info("📅 No assignmentDate provided, defaulting to {}");
        } else {
            log.info("📅 Using provided assignmentDate: {}");
        }

        List<Ride> rides = rideService.parseExcelFile(file, assignmentDate);
        log.info("✅ Processed {} rides from Excel");

        return ResponseEntity.ok("Upload successful, rides saved: " + rides.size());
    }

    // Assignment
    @PostMapping("/assign/today")
    public ResponseEntity<String> assignToday() {
        LocalDateTime start = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusDays(1);
        List<Ride> rides = rideRepository.findByPickupTimeBetween(start, end);
        List<Driver> drivers = driverRepository.findByActiveTrue();
        return ResponseEntity.ok("✅ Assignment attempt complete for today's rides");
    }

    @PostMapping("/schedule/auto")
    public ResponseEntity<String> autoSchedule(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "assignmentDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate assignmentDate
    ) throws IOException {
        if (assignmentDate == null) {
            assignmentDate = LocalDate.now(ZoneId.of("America/Denver")).plusDays(1);
        }

        log.info("📆 Scheduling for assignmentDate={}");

        List<Ride> rides = rideService.parseExcelFile(file, assignmentDate);
        if (rides.isEmpty()) {
            return ResponseEntity.badRequest().body("❌ No rides parsed from Excel for " + assignmentDate);
        }
        log.info("📝 Parsed {} rides for {}");

        rideService.optimizeSchedule(rides);

        LocalDateTime from = assignmentDate.atStartOfDay();
        LocalDateTime to = assignmentDate.atTime(23, 59, 59);
        var assignmentSummary = assignmentService.assignRides(from, to);

        if (assignmentSummary.isEmpty()) {
            log.info("⚠️ No assignments made for {}");
            return ResponseEntity.ok("⚠️ No drivers available or matching for the rides on " + assignmentDate);
        }

        return ResponseEntity.ok("✅ Scheduling complete for " + assignmentDate + ". Rides: " + rides.size() + ", Drivers assigned: " + assignmentSummary.size());
    }

    @GetMapping("/assign/summary")
    public List<DriverRideSummary> getDailySummary(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        return summaryService.getSummaryForDate(start, end);
    }

    // Audit Logs
    @GetMapping("/ride/{rideId}")
    public List<RideAudit> getAuditLogsByRide(@PathVariable Long rideId) {
        return rideAuditRepository.findByRideIdOrderByChangedAtDesc(rideId);
    }

    @GetMapping("/assignment/audit")
    public List<AssignmentAudit> getAuditLogs() {
        return assignmentAuditRepository.findAll(Sort.by(Sort.Direction.DESC, "assignmentTime"));
    }
}
