package com.mediroute.entity;

import jakarta.persistence.Embeddable;

@Embeddable
public class Location {
    private String address;
    private Float lat;
    private Float lng;
}
