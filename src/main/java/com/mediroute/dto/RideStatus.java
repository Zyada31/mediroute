package com.mediroute.dto;

import java.util.EnumSet;

public enum RideStatus {
    REQUESTED,        // Initial request received
    SCHEDULED,        // Ride scheduled but not assigned
    ASSIGNED,         // Assigned to driver(s)
    CONFIRMED,        // Driver confirmed assignment
    IN_PROGRESS,      // Pickup completed, en route
    AT_DESTINATION,   // Arrived at appointment
    WAITING,          // Waiting during appointment
    RETURNING,        // Return journey started
    COMPLETED,        // Ride fully completed
    CANCELLED,        // Cancelled before completion
    NO_SHOW,          // Patient didn't show up
    DELAYED           // Running behind schedule
}