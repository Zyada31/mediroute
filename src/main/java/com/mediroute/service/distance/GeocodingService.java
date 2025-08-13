package com.mediroute.service.distance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class GeocodingService {

    // Google API key must be provided via environment or secret store
    @Value("${google.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;

    public record GeoPoint(double lat, double lng) {
        public String toOSRMFormat() {
            return lng + "," + lat; // OSRM expects "lng,lat"
        }
    }

    @Cacheable(cacheNames = "geo:addr", key = "#address == null ? '' : #address.trim().toLowerCase()", unless = "#result == null")
    public GeoPoint geocode(String address) {
        if (address == null || address.isBlank()) return null;

        try {
            String encoded = URLEncoder.encode(address, StandardCharsets.UTF_8);
            String url = "https://maps.googleapis.com/maps/api/geocode/json?address=" + encoded + "&key=" + apiKey;

            Map<?, ?> response = restTemplate.getForObject(url, Map.class);
            if (response == null || response.get("results") == null) {
                log.warn("No response from Google Maps API for address: {}", address);
                return null;
            }

            List<?> results = (List<?>) response.get("results");
            if (results.isEmpty()) {
                log.warn("No geocoding result for address: {}", address);
                return null;
            }

            Map<?, ?> geometry = (Map<?, ?>) ((Map<?, ?>) results.get(0)).get("geometry");
            Map<?, ?> location = (Map<?, ?>) geometry.get("location");

            double lat = (Double) location.get("lat");
            double lng = (Double) location.get("lng");
            return new GeoPoint(lat, lng);

        } catch (Exception e) {
            log.error("❌ Failed to geocode address: {}", address, e);
            return null;
        }
    }
}