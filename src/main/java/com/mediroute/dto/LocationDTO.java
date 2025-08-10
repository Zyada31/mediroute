package com.mediroute.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

//@Embeddable
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//@Schema(description = "Geographic location with address and coordinates")
//public class Location {
//
//    @Column(name = "address")
//    @Schema(description = "Full address", example = "123 Main St, Denver, CO 80202")
//    private String address;
//
//    @Column(name = "latitude")
//    @Schema(description = "Latitude coordinate", example = "39.7392")
//    private Double latitude;
//
//    @Column(name = "longitude")
//    @Schema(description = "Longitude coordinate", example = "-104.9903")
//    private Double longitude;
//
//    public boolean isValid() {
//        return latitude != null && longitude != null &&
//                latitude >= -90 && latitude <= 90 &&
//                longitude >= -180 && longitude <= 180;
//    }
//
//    public String toOsrmFormat() {
//        if (!isValid()) return null;
//        return longitude + "," + latitude; // OSRM expects lng,lat
//    }
//}
/**
 * Location DTO for API requests/responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Geographic location")
public class LocationDTO {
    @NotBlank(message = "Address is required")
    @Schema(description = "Full address", example = "123 Main St, Denver, CO 80202")
    private String address;

    @Schema(description = "Latitude coordinate", example = "39.7392")
    @Min(-90) @Max(90)
    private Double latitude;

    @Schema(description = "Longitude coordinate", example = "-104.9903")
    @Min(-180) @Max(180)
    private Double longitude;

    @Schema(description = "Location type", example = "PICKUP")
    private LocationType type;

    @Schema(description = "Location notes")
    private String notes;

    public enum LocationType {
        PICKUP, DROPOFF, BASE, HOME, MEDICAL_FACILITY
    }

    public boolean isValid() {
        return latitude != null && longitude != null &&
                latitude >= -90 && latitude <= 90 &&
                longitude >= -180 && longitude <= 180;
    }

    public String toOsrmFormat() {
        return isValid() ? longitude + "," + latitude : null;
    }

    public static LocationDTO fromEntity(com.mediroute.entity.embeddable.Location location) {
        if (location == null) return null;
        return LocationDTO.builder()
                .address(location.getAddress())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .build();
    }
}