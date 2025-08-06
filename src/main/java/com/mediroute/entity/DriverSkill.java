package com.mediroute.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class DriverSkill {
    @Id
    @GeneratedValue
    private Long id;
    private String name;

    @ManyToOne
    private Driver driver;
}
