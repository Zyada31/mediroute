package com.mediroute.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import java.time.LocalDateTime;

@Entity
public class RideConstraint {
    @Id
    @GeneratedValue
    private Long id;

    @OneToOne
    private Ride ride;

    private LocalDateTime earliestPickup;
    private LocalDateTime latestDropoff;

    private boolean mustBeSolo; // e.g., special need
}