package com.mediroute.dto;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class GeoPoint {

    @Column(name = "base_lat", nullable = false)
    private double lat;

    @Column(name = "base_lng", nullable = false)
    private double lng;

    // Constructors
    public GeoPoint() {}

    public GeoPoint(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    // Getters and setters
    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    @Override
    public String toString() {
        return lat + "," + lng;
    }
}