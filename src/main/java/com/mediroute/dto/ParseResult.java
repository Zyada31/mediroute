package com.mediroute.dto;

import com.mediroute.entity.Ride;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ParseResult {
    private List<Ride> rides;
    private int skippedRows;
    private int totalRows;
    private boolean optimizationRan = false;
    private String optimizationError;

    public ParseResult(List<Ride> rides, int skippedRows, int totalRows) {
        this.rides = rides;
        this.skippedRows = skippedRows;
        this.totalRows = totalRows;
    }

    public int getSuccessfulRows() {
        return rides.size();
    }

    public double getSuccessRate() {
        return totalRows > 0 ? (rides.size() * 100.0) / (totalRows - 1) : 0.0; // -1 for header row
    }
}