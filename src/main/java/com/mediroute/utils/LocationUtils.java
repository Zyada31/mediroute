package com.mediroute.utils;

import com.mediroute.entity.Ride;
import com.mediroute.entity.embeddable.Location;
import com.mediroute.service.distance.GeocodingService;

public class LocationUtils {

    public static Location createLocationFromAddress(String address) {
        Location location = new Location();
        location.setAddress(address);
        return location;
    }

    public static Location createLocationWithCoordinates(String address, double lat, double lng) {
        Location location = new Location();
        location.setAddress(address);
        location.setLatitude(lat);
        location.setLongitude(lng);
        return location;
    }

    public static void geocodeLocation(Location location, GeocodingService geocodingService) {
        if (location == null || location.getAddress() == null) return;

        GeocodingService.GeoPoint geoPoint = geocodingService.geocode(location.getAddress());
        if (geoPoint != null) {
            location.setLatitude(geoPoint.lat());
            location.setLongitude(geoPoint.lng());
        }
    }

    public static double calculateDistance(Location from, Location to) {
        if (from == null || to == null || !from.isValid() || !to.isValid()) {
            return 0.0;
        }

        double lat1 = Math.toRadians(from.getLatitude());
        double lon1 = Math.toRadians(from.getLongitude());
        double lat2 = Math.toRadians(to.getLatitude());
        double lon2 = Math.toRadians(to.getLongitude());

        double dlat = lat2 - lat1;
        double dlon = lon2 - lon1;

        double a = Math.sin(dlat/2) * Math.sin(dlat/2) +
                Math.cos(lat1) * Math.cos(lat2) * Math.sin(dlon/2) * Math.sin(dlon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        return 6371 * c; // Earth's radius in kilometers
    }

    /**
     * Safely get coordinates for OSRM format
     */
    public static String getOsrmCoordinates(Ride ride, boolean isPickup) {
        Location location = isPickup ? ride.getPickupLocation() : ride.getDropoffLocation();
        if (location == null || !location.isValid()) {
            return null;
        }
        return location.toOsrmFormat();
    }

    /**
     * Check if ride has all required coordinates
     */
    public static boolean hasCompleteLocationData(Ride ride) {
        return ride.getPickupLocation() != null &&
                ride.getPickupLocation().isValid() &&
                ride.getDropoffLocation() != null &&
                ride.getDropoffLocation().isValid();
    }
}