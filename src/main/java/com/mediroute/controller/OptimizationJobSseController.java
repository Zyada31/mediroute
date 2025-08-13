package com.mediroute.controller;

import com.mediroute.entity.OptimizationJob;
import com.mediroute.repository.OptimizationJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/optimization-jobs")
@RequiredArgsConstructor
public class OptimizationJobSseController {

    private final OptimizationJobRepository jobs;

    @GetMapping(path = "/{jobId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public Flux<ServerSentEvent<Map<String, Object>>> streamJob(@PathVariable Long jobId) {
        return Flux.interval(Duration.ofSeconds(1))
                .map(tick -> jobs.findById(jobId).orElse(null))
                .takeUntil(job -> job != null && (job.getStatus() == OptimizationJob.JobStatus.COMPLETED || job.getStatus() == OptimizationJob.JobStatus.FAILED))
                .map(job -> {
                    if (job == null) {
                        return ServerSentEvent.<Map<String,Object>>builder()
                                .event("heartbeat").build();
                    }
                    Map<String,Object> payload = Map.of(
                            "jobId", job.getId(),
                            "status", job.getStatus().name(),
                            "batchId", job.getBatchId(),
                            "error", job.getError()
                    );
                    return ServerSentEvent.<Map<String,Object>>builder()
                            .event("job-status")
                            .data(payload)
                            .build();
                });
    }
}


