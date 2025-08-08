package com.mediroute.service.distance;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class GeocodingService {

    @Value("${google.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final Map<String, GeoPoint> cache = new ConcurrentHashMap<>();

    public record GeoPoint(double lat, double lng) {
        public String toOSRMFormat() {
            return lng + "," + lat; // OSRM expects "lng,lat"
        }
    }

    public GeoPoint geocode(String address) {
        if (address == null || address.isBlank()) return null;

        // Return from cache if available
        if (cache.containsKey(address)) {
            return cache.get(address);
        }

        try {
            String encoded = URLEncoder.encode(address, StandardCharsets.UTF_8);
            log.info("Google Maps API key: {}", apiKey);

            String url = "https://maps.googleapis.com/maps/api/geocode/json?address=" + encoded + "&key=" + apiKey;
            log.info("Google Maps API for address: {}", url);

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
            GeoPoint point = new GeoPoint(lat, lng);

            cache.put(address, point);
            return point;

        } catch (Exception e) {
            log.error("❌ Failed to geocode address: {}", address, e);
            return null;
        }
    }
}