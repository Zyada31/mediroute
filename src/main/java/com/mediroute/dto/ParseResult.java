package com.mediroute.dto;

import com.mediroute.entity.Ride;
import com.mediroute.service.ride.EnhancedMedicalTransportOptimizer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@Builder
@Schema(description = "Parse result from Excel/CSV processing")
public class ParseResult {
    private final List<Ride> rides;
    private final int skippedRows;
    private final int totalRows;
    private final int successfulRows;
    private final double successRate;

    @Schema(description = "Whether optimization was run")
    private Boolean optimizationRan = false;

    @Schema(description = "Optimization error if any")
    private String optimizationError;

    @Schema(description = "Optimization result if run")
    private EnhancedMedicalTransportOptimizer.OptimizationResult optimizationResult;

    // Constructor for your existing code
    public ParseResult(List<Ride> rides, int skipped, int total) {
        this.rides = rides;
        this.skippedRows = skipped;
        this.totalRows = total;
        this.successfulRows = rides.size();
        this.successRate = total > 0 ? (rides.size() * 100.0) / total : 0.0;
    }

    public static ParseResult create(List<Ride> rides, int skipped, int total) {
        int successful = rides.size();
        double rate = total > 0 ? (successful * 100.0) / total : 0.0;

        return ParseResult.builder()
                .rides(rides)
                .skippedRows(skipped)
                .totalRows(total)
                .successfulRows(successful)
                .successRate(rate)
                .build();
    }
}