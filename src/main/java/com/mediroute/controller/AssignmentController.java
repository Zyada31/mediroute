package com.mediroute.controller;

import com.mediroute.DTO.DriverRideSummary;
import com.mediroute.service.AssignmentSummaryService;
import com.mediroute.service.DriverAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/assign")
@RequiredArgsConstructor
public class AssignmentController {

    private final DriverAssignmentService assignmentService;
    private final AssignmentSummaryService summaryService;

    @PostMapping("/today")
    public ResponseEntity<String> assignToday() {
        LocalDateTime start = LocalDateTime.now().withHour(0).withMinute(0);
        LocalDateTime end = start.plusDays(1);
        assignmentService.assignDriversForDate(start, end);
        return ResponseEntity.ok("✅ Assignment attempt complete for today's rides");
    }


    @GetMapping("/summary")
    public List<DriverRideSummary> getDailySummary(@RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        return summaryService.getSummaryForDate(start, end);
    }
}