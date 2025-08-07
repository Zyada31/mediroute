package com.mediroute.utils;

public class CoordinateNormalizer {

    public static String normalize(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("❌ Empty coordinate input");
        }

        if (input.matches("^-?\\d+(\\.\\d+)?,-?\\d+(\\.\\d+)?$")) {
            String[] parts = input.split(",");
            double a = Double.parseDouble(parts[0].trim());
            double b = Double.parseDouble(parts[1].trim());

            if (Math.abs(a) <= 90 && Math.abs(b) <= 180) {
                // lat,lng → flip to lng,lat
                return b + "," + a;
            } else {
                // already lng,lat
                return a + "," + b;
            }
        }
        return input; // probably an address string → let geocoding handle
    }
}