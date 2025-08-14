package com.mediroute.service.ride;

import com.mediroute.dto.OptimizationResult;
import com.mediroute.entity.OptimizationJob;
import com.mediroute.repository.OptimizationJobRepository;
import lombok.RequiredArgsConstructor;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OptimizationJobService {

    private final OptimizationJobRepository jobs;
    private final OptimizationIntegrationService optimizationService;
    private final MeterRegistry meterRegistry;
    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional
    public OptimizationJob submitForDate(LocalDate date) {
        OptimizationJob job = new OptimizationJob();
        job.setType(OptimizationJob.JobType.DATE);
        job.setDate(date);
        job.setStatus(OptimizationJob.JobStatus.PENDING);
        job.setSubmittedAt(LocalDateTime.now());
        return jobs.save(job);
    }

    @Transactional
    public OptimizationJob submitForRides(List<Long> rideIds) {
        OptimizationJob job = new OptimizationJob();
        job.setType(OptimizationJob.JobType.RIDES);
        job.setRideIdsCsv(rideIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        job.setStatus(OptimizationJob.JobStatus.PENDING);
        job.setSubmittedAt(LocalDateTime.now());
        return jobs.save(job);
    }

    @Async
    @Transactional
    public void runJobAsync(Long jobId) {
        OptimizationJob job = jobs.findById(jobId).orElseThrow();
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            job.setStatus(OptimizationJob.JobStatus.RUNNING);
            job.setStartedAt(LocalDateTime.now());
            jobs.save(job);

            OptimizationResult result;
            if (job.getType() == OptimizationJob.JobType.DATE) {
                result = optimizationService.optimizeRidesForDate(job.getDate());
            } else {
                List<Long> rideIds = Arrays.stream(job.getRideIdsCsv().split(","))
                        .filter(s -> !s.isBlank())
                        .map(Long::valueOf)
                        .collect(Collectors.toList());
                result = optimizationService.optimizeSpecificRides(rideIds);
            }

            job.setBatchId(result.getBatchId());
            job.setStatus(OptimizationJob.JobStatus.COMPLETED);
            job.setCompletedAt(LocalDateTime.now());
            jobs.save(job);
            sample.stop(Timer.builder("optimizer.job.duration").tag("status","completed").register(meterRegistry));

            notifyWebhook(job);
        } catch (Exception e) {
            log.error("Optimization job {} failed: {}", jobId, e.getMessage(), e);
            job.setStatus(OptimizationJob.JobStatus.FAILED);
            job.setError(e.getMessage());
            job.setCompletedAt(LocalDateTime.now());
            jobs.save(job);
            sample.stop(Timer.builder("optimizer.job.duration").tag("status","failed").register(meterRegistry));

            notifyWebhook(job);
        }
    }

    private void notifyWebhook(OptimizationJob job) {
        try {
            if (job.getCallbackUrl() == null || job.getCallbackUrl().isBlank()) return;
            Map<String, Object> payload = Map.of(
                    "jobId", job.getId(),
                    "status", job.getStatus().name(),
                    "batchId", job.getBatchId(),
                    "error", job.getError(),
                    "submittedAt", String.valueOf(job.getSubmittedAt()),
                    "startedAt", String.valueOf(job.getStartedAt()),
                    "completedAt", String.valueOf(job.getCompletedAt())
            );
            restTemplate.postForEntity(job.getCallbackUrl(), payload, Void.class);
        } catch (Exception ex) {
            log.warn("Failed to notify webhook for job {}: {}", job.getId(), ex.getMessage());
        }
    }
}


