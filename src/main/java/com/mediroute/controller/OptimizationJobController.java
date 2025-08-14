package com.mediroute.controller;

import com.mediroute.entity.OptimizationJob;
import com.mediroute.repository.OptimizationJobRepository;
import com.mediroute.service.ride.OptimizationJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/optimization-jobs")
@RequiredArgsConstructor
public class OptimizationJobController {

    private final OptimizationJobService jobService;
    private final OptimizationJobRepository jobs;
    private final StringRedisTemplate redis;

    @PostMapping("/submit/date/{date}")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public ResponseEntity<Map<String, Object>> submitForDate(@PathVariable LocalDate date,
                                                             @RequestParam(required = false) String callbackUrl,
                                                             @RequestHeader(value = "Idempotency-Key", required = false) String idemKey) {
        if (idemKey != null && !idemKey.isBlank()) {
            String key = "idem:job:date:" + date + ":" + idemKey;
            Boolean ok = redis.opsForValue().setIfAbsent(key, "1", java.time.Duration.ofHours(24));
            if (Boolean.FALSE.equals(ok)) {
                return ResponseEntity.status(208).body(Map.of("status", "duplicate"));
            }
        }
        OptimizationJob job = jobService.submitForDate(date);
        if (callbackUrl != null && !callbackUrl.isBlank()) job.setCallbackUrl(callbackUrl);
        jobs.save(job);
        jobService.runJobAsync(job.getId());
        return ResponseEntity.accepted().body(Map.of("jobId", job.getId(), "status", job.getStatus()));
    }

    @PostMapping("/submit/rides")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public ResponseEntity<Map<String, Object>> submitForRides(@RequestBody List<Long> rideIds,
                                                              @RequestParam(required = false) String callbackUrl,
                                                              @RequestHeader(value = "Idempotency-Key", required = false) String idemKey) {
        if (idemKey != null && !idemKey.isBlank()) {
            String key = "idem:job:rides:" + rideIds.hashCode() + ":" + idemKey;
            Boolean ok = redis.opsForValue().setIfAbsent(key, "1", java.time.Duration.ofHours(24));
            if (Boolean.FALSE.equals(ok)) {
                return ResponseEntity.status(208).body(Map.of("status", "duplicate"));
            }
        }
        OptimizationJob job = jobService.submitForRides(rideIds);
        if (callbackUrl != null && !callbackUrl.isBlank()) job.setCallbackUrl(callbackUrl);
        jobs.save(job);
        jobService.runJobAsync(job.getId());
        return ResponseEntity.accepted().body(Map.of("jobId", job.getId(), "status", job.getStatus()));
    }

    @GetMapping("/{jobId}")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public ResponseEntity<OptimizationJob> getStatus(@PathVariable Long jobId) {
        return jobs.findById(jobId).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}


