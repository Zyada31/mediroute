package com.mediroute.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "rides")
public class Ride {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", insertable = false, updatable = false)
    private Driver driver;

    @Column(name = "driver_id")
    private Long driverId;

    @Column(name = "pickup_location", nullable = false)
    private String pickupLocation;

    @Column(name = "dropoff_location", nullable = false)
    private String dropoffLocation;

    @Column(name = "pickup_time", nullable = false)
    private LocalDateTime pickupTime;

    @Column(name = "wait_time", columnDefinition = "INT DEFAULT 0 CHECK (wait_time BETWEEN 0 AND 15)")
    private Integer waitTime;

    @Column(name = "is_sequential", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isSequential;

    @Column(name = "distance")
    private Float distance;

    @Column(name = "status", columnDefinition = "VARCHAR(20) CHECK (status IN ('scheduled', 'in_progress', 'completed', 'canceled')) DEFAULT 'scheduled'")
    private String status;

    @Column(name = "required_vehicle_type", columnDefinition = "VARCHAR(20) CHECK (required_vehicle_type IN ('sedan', 'van', 'wheelchair_van'))")
    private String requiredVehicleType;

//    @JdbcTypeCode(SqlTypes.JSON)
//    @Column(name = "required_skills", columnDefinition = "jsonb", nullable = false)
//    private Map<String, Object> requiredSkills = new HashMap<>();

    @Transient
    private LocalTime timeOnly;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> requiredSkills;
}