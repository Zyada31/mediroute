package com.mediroute.service.distance;

import java.util.List;

public interface DistanceService {
    double[][] getDistanceMatrix(List<String> locations);

    /**
     * Returns distance in meters between two locations.
     */
    int getDistanceInMeters(String origin, String destination);

    /**
     * Returns a distance matrix [origins.size()][destinations.size()] in meters.
     */
    int[][] getDistanceMatrix(List<String> origins, List<String> destinations);
}

