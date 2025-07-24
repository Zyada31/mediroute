package com.mediroute.controller;

import com.mediroute.entity.Ride;
import com.mediroute.service.DriverAssignmentService;
import com.mediroute.service.RideService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schedule")
public class ScheduleController {

    private final RideService rideService;
    private final DriverAssignmentService driverAssignmentService;

    @PostMapping("/auto")
    public ResponseEntity<String> autoSchedule(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "assignmentDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate assignmentDate
    ) throws IOException {

        // 1. Default assignment date to tomorrow (MST) if not provided
        if (assignmentDate == null) {
            assignmentDate = LocalDate.now(ZoneId.of("America/Denver")).plusDays(1);
        }

        log.info("📆 Scheduling for assignmentDate={}", assignmentDate);

        // 2. Parse Excel and create Ride/Patient records
        List<Ride> rides = rideService.parseExcelFile(file, assignmentDate);
        log.info("📝 Parsed {} rides for {}", rides.size(), assignmentDate);

        // 3. Optimize ride sequence (using placeholder driverId)
        rideService.optimizeSchedule(rides);

        // 4. Assign drivers to rides in that date range
        LocalDateTime from = assignmentDate.atStartOfDay();
        LocalDateTime to = assignmentDate.atTime(23, 59, 59);
        driverAssignmentService.assignDriversForDate(from, to);

        return ResponseEntity.ok("✅ Scheduling complete for " + assignmentDate + ". Total rides: " + rides.size());
    }
}