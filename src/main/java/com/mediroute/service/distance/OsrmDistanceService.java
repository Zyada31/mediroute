//package com.mediroute.service.distance;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.mediroute.utils.CoordinateNormalizer;
//import lombok.RequiredArgsConstructor;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//
//import java.net.URLEncoder;
//import java.nio.charset.StandardCharsets;
//import java.util.*;
//
//
//@Service
//@RequiredArgsConstructor
//public class OsrmDistanceService implements DistanceService {
//    Logger log = LoggerFactory.getLogger(OsrmDistanceService.class);
//
//    static {
//        System.loadLibrary("jniortools");
//    }
//
//    private final RestTemplate restTemplate;
//    private final ObjectMapper objectMapper;
//    private final GeocodingService geocodingService;
//
//    @Value("${osrm.base-url:http://localhost:5000}")
//    private String osrmBaseUrl;
//
//    // ✅ Safe default OSRM table limit (can override with config if your OSRM build supports more)
//    private static final int OSRM_MAX_POINTS = 100;
//
//    @Override
//    public double[][] getDistanceMatrix(List<String> locations) {
//        try {
//            if (locations == null || locations.isEmpty()) {
//                throw new IllegalArgumentException("❌ Location list is empty or null.");
//            }
//
//            final int MAX_OSRM_POINTS = 100; // safe threshold
//            int total = locations.size();
//            double[][] fullMatrix = new double[total][total];
//
//            // Build normalized lng,lat coords
//            List<String> coords = new ArrayList<>();
//            for (String loc : locations) {
//                String coord;
//                if (loc.matches("^-?\\d+(\\.\\d+)?,-?\\d+(\\.\\d+)?$")) {
//                    coord = CoordinateNormalizer.normalize(loc);
//                } else {
//                    GeocodingService.GeoPoint geo = geocodingService.geocode(loc);
//                    if (geo == null) {
//                        throw new IllegalArgumentException("❌ Failed to geocode location: " + loc);
//                    }
//                    coord = geo.lng() + "," + geo.lat();
//                }
//                coords.add(coord);
//            }
//
//            // Batch rows into ≤ MAX_OSRM_POINTS
//            for (int i = 0; i < total; i += MAX_OSRM_POINTS) {
//                int iEnd = Math.min(i + MAX_OSRM_POINTS, total);
//                for (int j = 0; j < total; j += MAX_OSRM_POINTS) {
//                    int jEnd = Math.min(j + MAX_OSRM_POINTS, total);
//
//                    // Extract sub-matrix coordinates
//                    List<String> subOrigins = coords.subList(i, iEnd);
//                    List<String> subDestinations = coords.subList(j, jEnd);
//
//                    String url = osrmBaseUrl + "/table/v1/driving/" +
//                            String.join(";", subOrigins) + ";" + String.join(";", subDestinations) +
//                            "?annotations=distance&sources=0";
//
//                    log.info("📡 OSRM batch [{}-{}]x[{}-{}]: {}", i, iEnd, j, jEnd, url);
//
//                    String response = restTemplate.getForObject(url, String.class);
//                    JsonNode json = objectMapper.readTree(response);
//                    JsonNode distances = json.get("distances");
//
//                    if (distances == null || !distances.isArray()) {
//                        throw new RuntimeException("❌ Invalid OSRM response: 'distances' missing");
//                    }
//
//                    // Fill fullMatrix
//                    for (int r = 0; r < distances.size(); r++) {
//                        int globalRow = i + r;
//                        if (globalRow >= total) break;
//
//                        for (int c = 0; c < distances.get(r).size(); c++) {
//                            int globalCol = j + c;
//                            if (globalCol >= total) break;
//
//                            fullMatrix[globalRow][globalCol] = distances.get(r).get(c).asDouble();
//                        }
//                    }
//                }
//            }
//
//            return fullMatrix;
//
//        } catch (Exception e) {
//            log.error("❌ Failed to fetch distance matrix from OSRM", e);
//            throw new RuntimeException("OSRM distance matrix error", e);
//        }
//    }
//
//    private double[][] getMatrixChunk(List<String> locations) {
//        try {
//            List<String> finalCoords = new ArrayList<>();
//            Map<String, List<Integer>> coordIndexMap = new HashMap<>();
//            double baseLat = Double.NaN, baseLng = Double.NaN;
//
//            for (int i = 0; i < locations.size(); i++) {
//                String loc = locations.get(i);
//                String coord;
//
//                if (loc.matches("^-?\\d+(\\.\\d+)?,-?\\d+(\\.\\d+)?$")) {
//                    coord = CoordinateNormalizer.normalize(loc);
//                    String[] parts = coord.split(",");
//                    baseLng = Double.parseDouble(parts[0]);
//                    baseLat = Double.parseDouble(parts[1]);
//                } else {
//                    GeocodingService.GeoPoint geo = geocodingService.geocode(loc);
//                    if (geo == null) throw new IllegalArgumentException("❌ Failed to geocode location: " + loc);
//                    baseLat = geo.lat();
//                    baseLng = geo.lng();
//                    coord = geo.lng() + "," + geo.lat();
//                }
//
//                finalCoords.add(coord);
//                coordIndexMap.computeIfAbsent(coord, k -> new ArrayList<>()).add(i);
//            }
//
//            List<String> distinctCoords = new ArrayList<>(new LinkedHashSet<>(finalCoords));
//
//            if (distinctCoords.size() == 1 && locations.size() > 1) {
//                log.warn("⚠️ All coords identical → applying jitter");
//                distinctCoords.clear();
//                coordIndexMap.clear();
//
//                for (int i = 0; i < locations.size(); i++) {
//                    double jitterLat = baseLat + (Math.random() - 0.5) / 10000;
//                    double jitterLng = baseLng + (Math.random() - 0.5) / 10000;
//                    String jittered = jitterLng + "," + jitterLat;
//                    distinctCoords.add(jittered);
//                    coordIndexMap.computeIfAbsent(jittered, k -> new ArrayList<>()).add(i);
//                }
//            }
//
//            String coordString = String.join(";", distinctCoords);
//            String url = osrmBaseUrl + "/table/v1/driving/" + coordString + "?annotations=distance";
//            log.info("📡 Requesting OSRM distance matrix with {} points: {}", distinctCoords.size(), url);
//
//            String response = restTemplate.getForObject(url, String.class);
//            JsonNode json = objectMapper.readTree(response);
//            JsonNode distances = json.get("distances");
//
//            if (distances == null || !distances.isArray()) {
//                throw new RuntimeException("❌ Invalid OSRM response: 'distances' missing");
//            }
//
//            int size = locations.size();
//            double[][] matrix = new double[size][size];
//
//            for (Map.Entry<String, List<Integer>> entryI : coordIndexMap.entrySet()) {
//                int iIndex = distinctCoords.indexOf(entryI.getKey());
//                for (Map.Entry<String, List<Integer>> entryJ : coordIndexMap.entrySet()) {
//                    int jIndex = distinctCoords.indexOf(entryJ.getKey());
//                    double dist = distances.get(iIndex).get(jIndex).asDouble();
//
//                    for (int i : entryI.getValue()) {
//                        for (int j : entryJ.getValue()) {
//                            matrix[i][j] = dist;
//                        }
//                    }
//                }
//            }
//
//            return matrix;
//        } catch (Exception e) {
//            log.error("❌ Failed to fetch distance matrix from OSRM", e);
//            throw new RuntimeException("OSRM distance matrix error", e);
//        }
//    }
//
//    @Override
//    public int getDistanceInMeters(String origin, String destination) {
//        try {
//            String coords = URLEncoder.encode(origin, StandardCharsets.UTF_8) + ";" +
//                    URLEncoder.encode(destination, StandardCharsets.UTF_8);
//            String url = osrmBaseUrl + "/route/v1/driving/" + coords + "?overview=false";
//            String response = restTemplate.getForObject(url, String.class);
//            JsonNode json = objectMapper.readTree(response);
//
//            return json.get("routes").get(0).get("distance").asInt();
//        } catch (Exception e) {
//            log.error("❌ Failed to fetch single distance from OSRM", e);
//            throw new RuntimeException("OSRM distance fetch error", e);
//        }
//    }
//
//    @Override
//    public int[][] getDistanceMatrix(List<String> origins, List<String> destinations) {
//        try {
//            String allCoords = String.join(";", origins) + ";" + String.join(";", destinations);
//            String url = osrmBaseUrl + "/table/v1/driving/" +
//                    URLEncoder.encode(allCoords, StandardCharsets.UTF_8) +
//                    "?annotations=distance&sources=0";
//
//            String response = restTemplate.getForObject(url, String.class);
//            JsonNode json = objectMapper.readTree(response);
//            JsonNode distances = json.get("distances");
//
//            int rows = distances.size();
//            int cols = distances.get(0).size();
//            int[][] matrix = new int[rows][cols];
//
//            for (int i = 0; i < rows; i++) {
//                for (int j = 0; j < cols; j++) {
//                    matrix[i][j] = distances.get(i).get(j).asInt();
//                }
//            }
//            return matrix;
//        } catch (Exception e) {
//            log.error("❌ Failed to fetch many-to-many matrix from OSRM", e);
//            throw new RuntimeException("OSRM batch distance matrix error", e);
//        }
//    }
//}

package com.mediroute.service.distance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediroute.utils.CoordinateNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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

    // Remove the static native library loading that causes issues
    // static {
    //     System.loadLibrary("jniortools");
    // }

    @Override
    public double[][] getDistanceMatrix(List<String> locations) {
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
    public int getDistanceInMeters(String origin, String destination) {
        try {
            String coords = URLEncoder.encode(origin, StandardCharsets.UTF_8) + ";" +
                    URLEncoder.encode(destination, StandardCharsets.UTF_8);
            String url = osrmBaseUrl + "/route/v1/driving/" + coords + "?overview=false";
            String response = restTemplate.getForObject(url, String.class);
            JsonNode json = objectMapper.readTree(response);

            return json.get("routes").get(0).get("distance").asInt();
        } catch (Exception e) {
            log.error("❌ Failed to fetch single distance from OSRM: {}", e.getMessage());
            return 10000; // 10km default
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
            log.error("❌ Failed to fetch many-to-many matrix from OSRM: {}", e.getMessage());

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
