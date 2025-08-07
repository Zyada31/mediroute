package com.mediroute.exceptions;

public class GeocodingException extends RuntimeException {
    private final String address;

    public GeocodingException(String address, String message) {
        super("Failed to geocode address '" + address + "': " + message);
        this.address = address;
    }

    public GeocodingException(String address, Throwable cause) {
        super("Failed to geocode address '" + address + "'", cause);
        this.address = address;
    }

    public String getAddress() {
        return address;
    }
}
