package com.mediroute.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashMap;
import java.util.Map;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "patients")
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "contact_info")
    private String contactInfo;

    @Column(name = "default_pickup_location")
    private String defaultPickupLocation;

    @Column(name = "default_dropoff_location")
    private String defaultDropoffLocation;

    @Column(name = "requires_wheelchair", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean requiresWheelchair;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "special_needs", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> specialNeeds = new HashMap<>();
}