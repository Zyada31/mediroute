package com.mediroute.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Geographic location with address and coordinates")
public class Location {

    @Column(name = "address")
    @Schema(description = "Full address", example = "123 Main St, Denver, CO 80202")
    private String address;

    @Column(name = "latitude")
    @Schema(description = "Latitude coordinate", example = "39.7392")
    private Double latitude;

    @Column(name = "longitude")
    @Schema(description = "Longitude coordinate", example = "-104.9903")
    private Double longitude;

    public boolean isValid() {
        return latitude != null && longitude != null &&
                latitude >= -90 && latitude <= 90 &&
                longitude >= -180 && longitude <= 180;
    }

    public String toOsrmFormat() {
        if (!isValid()) return null;
        return longitude + "," + latitude; // OSRM expects lng,lat
    }
}
