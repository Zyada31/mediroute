package com.mediroute.service.distance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediroute.utils.CoordinateNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.cache.annotation.Cacheable;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OsrmDistanceService implements DistanceService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final GeocodingService geocodingService;

    @Value("${osrm.base-url:http://localhost:5000}")
    private String osrmBaseUrl;

    private static final int OSRM_MAX_POINTS = 100;

    private static boolean orToolsAvailable = false;

    static {
        try {
            System.loadLibrary("jniortools");
            orToolsAvailable = true;
            log.info("✅ OR-Tools loaded successfully");
        } catch (UnsatisfiedLinkError e) {
            log.warn("⚠️ OR-Tools not available, using fallback algorithm");
            orToolsAvailable = false;
        }
    }

    /**
     * Lightweight health check to verify OSRM is reachable.
     */
    public boolean isOsrmHealthy() {
        try {
            // Probe a lightweight valid endpoint; Denver coords within our sample map
            String url = osrmBaseUrl + "/nearest/v1/driving/-104.9903,39.7392";
            restTemplate.getForObject(url, String.class);
            return true;
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            // Any 4xx means OSRM responded → consider reachable
            if (e.getStatusCode().is4xxClientError()) return true;
            log.warn("OSRM health check failed at {}: {}", osrmBaseUrl, e.getMessage());
            return false;
        } catch (Exception e) {
            log.warn("OSRM health check failed at {}: {}", osrmBaseUrl, e.getMessage());
            return false;
        }
    }


    @Override
    public double[][] getDistanceMatrix(List<String> locations) {
//        if (!orToolsAvailable || locations.size() > 100) {
//            return getFallbackDistanceMatrix(locations);
//        }
        try {
            if (locations == null || locations.isEmpty()) {
                throw new IllegalArgumentException("❌ Location list is empty or null.");
            }

            int total = locations.size();
            double[][] fullMatrix = new double[total][total];

            List<String> coords = new ArrayList<>();
            for (String loc : locations) {
                String coord;
                if (loc.matches("^-?\\d+(\\.\\d+)?,-?\\d+(\\.\\d+)?$")) {
                    coord = CoordinateNormalizer.normalize(loc);
                } else {
                    GeocodingService.GeoPoint geo = geocodingService.geocode(loc);
                    if (geo == null) {
                        throw new IllegalArgumentException("❌ Failed to geocode location: " + loc);
                    }
                    coord = geo.lng() + "," + geo.lat();
                }
                coords.add(coord);
            }

            // Handle batching for large matrices
            for (int i = 0; i < total; i += OSRM_MAX_POINTS) {
                int iEnd = Math.min(i + OSRM_MAX_POINTS, total);
                for (int j = 0; j < total; j += OSRM_MAX_POINTS) {
                    int jEnd = Math.min(j + OSRM_MAX_POINTS, total);

                    List<String> subOrigins = coords.subList(i, iEnd);
                    List<String> subDestinations = coords.subList(j, jEnd);

                    String coordString = String.join(";", subOrigins);
                    if (!subOrigins.equals(subDestinations)) {
                        coordString += ";" + String.join(";", subDestinations);
                    }

                    String url = osrmBaseUrl + "/table/v1/driving/" + coordString + "?annotations=distance";

                    try {
                        String response = restTemplate.getForObject(url, String.class);
                        JsonNode json = objectMapper.readTree(response);
                        JsonNode distances = json.get("distances");

                        if (distances == null || !distances.isArray()) {
                            log.warn("Invalid OSRM response for batch [{}-{}]x[{}-{}]", i, iEnd, j, jEnd);
                            continue;
                        }

                        // Fill fullMatrix
                        for (int r = 0; r < distances.size(); r++) {
                            int globalRow = i + r;
                            if (globalRow >= total) break;

                            for (int c = 0; c < distances.get(r).size(); c++) {
                                int globalCol = j + c;
                                if (globalCol >= total) break;

                                fullMatrix[globalRow][globalCol] = distances.get(r).get(c).asDouble();
                            }
                        }
                    } catch (Exception e) {
                        log.error("❌ Failed to get distance matrix for batch [{}-{}]x[{}-{}]: {}",
                                i, iEnd, j, jEnd, e.getMessage());
                        // Fill with default values
                        for (int r = i; r < iEnd; r++) {
                            for (int c = j; c < jEnd; c++) {
                                if (r < total && c < total) {
                                    fullMatrix[r][c] = r == c ? 0.0 : 10000.0; // Default distance
                                }
                            }
                        }
                    }
                }
            }

            return fullMatrix;

        } catch (Exception e) {
            log.error("❌ Failed to fetch distance matrix from OSRM", e);

            // Return a default matrix
            int size = locations.size();
            double[][] defaultMatrix = new double[size][size];
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    defaultMatrix[i][j] = i == j ? 0.0 : 10000.0; // 10km default
                }
            }
            return defaultMatrix;
        }
    }

    @Override
    @Cacheable(cacheNames = "osrm:distance", key = "#root.target.normalize(#origin) + '->' + #root.target.normalize(#destination)")
    @CircuitBreaker(name = "osrm", fallbackMethod = "fallbackDistance")
    @Retry(name = "osrm")
    @RateLimiter(name = "osrm")
    @Bulkhead(name = "osrm")
    public int getDistanceInMeters(String origin, String destination) {
        try {
            String coords = URLEncoder.encode(origin, StandardCharsets.UTF_8) + ";" +
                    URLEncoder.encode(destination, StandardCharsets.UTF_8);
            String url = osrmBaseUrl + "/route/v1/driving/" + coords + "?overview=false";
            String response = restTemplate.getForObject(url, String.class);
            JsonNode json = objectMapper.readTree(response);

            return json.get("routes").get(0).get("distance").asInt();
        } catch (Exception e) {
            log.warn("OSRM distance fallback for single-leg: {}", e.getMessage());
            return 10000; // 10km default
        }
    }

    // Fallback signature must match method args plus Throwable at the end
    private int fallbackDistance(String origin, String destination, Throwable t) {
        log.warn("Using fallback distance for {} -> {} due to: {}", origin, destination, t.toString());
        return 10000;
    }

    // Normalize coordinates (round to ~5 decimal places ~1m) to improve cache hit rate
    public String normalize(String coord) {
        try {
            String[] parts = coord.split(",");
            double lat = Double.parseDouble(parts[0]);
            double lng = Double.parseDouble(parts[1]);
            return String.format(java.util.Locale.US, "%.5f,%.5f", lat, lng);
        } catch (Exception e) {
            return coord;
        }
    }

    @Override
    public int[][] getDistanceMatrix(List<String> origins, List<String> destinations) {
        try {
            String allCoords = String.join(";", origins) + ";" + String.join(";", destinations);
            String url = osrmBaseUrl + "/table/v1/driving/" +
                    URLEncoder.encode(allCoords, StandardCharsets.UTF_8) +
                    "?annotations=distance&sources=0";

            String response = restTemplate.getForObject(url, String.class);
            JsonNode json = objectMapper.readTree(response);
            JsonNode distances = json.get("distances");

            int rows = distances.size();
            int cols = distances.get(0).size();
            int[][] matrix = new int[rows][cols];

            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    matrix[i][j] = distances.get(i).get(j).asInt();
                }
            }
            return matrix;
        } catch (Exception e) {
            log.warn("OSRM distance fallback for matrix: {}", e.getMessage());

            // Return default matrix
            int[][] defaultMatrix = new int[origins.size()][destinations.size()];
            for (int i = 0; i < origins.size(); i++) {
                for (int j = 0; j < destinations.size(); j++) {
                    defaultMatrix[i][j] = 10000; // 10km default
                }
            }
            return defaultMatrix;
        }
    }
}
