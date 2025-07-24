package com.mediroute.controller;

import com.mediroute.entity.Ride;
import com.mediroute.repository.RideRepository;
import com.mediroute.service.RideService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
@CrossOrigin(origins = "http://localhost:3000")
@Slf4j
@RestController
@RequestMapping("/api/rides")
@RequiredArgsConstructor
public class RideController {

    private final RideService rideService;
    private final RideRepository rideRepository;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "assignmentDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate assignmentDate
    ) throws IOException {

        log.info("📥 Received Excel upload with size: {}", file.getSize());

        if (assignmentDate == null) {
            assignmentDate = LocalDate.now().plusDays(1); // Default to tomorrow
            log.info("📅 No assignmentDate provided, defaulting to {}", assignmentDate);
        } else {
            log.info("📅 Using provided assignmentDate: {}", assignmentDate);
        }

        List<Ride> rides = rideService.parseExcelFile(file, assignmentDate);
        log.info("✅ Processed {} rides from Excel", rides.size());

        return ResponseEntity.ok("Upload successful, rides saved: " + rides.size());
    }

//    @PostMapping("/assign")
//    public ResponseEntity<String> assignDriversToScheduledRides() {
//        List<Ride> rides = rideRepository.findByStatus("scheduled");
//        rideService.assignDrivers(rides);
//        return ResponseEntity.ok("✅ Assigned drivers to " + rides.size() + " scheduled rides.");
//    }
}