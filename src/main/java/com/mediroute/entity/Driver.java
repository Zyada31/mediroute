package com.mediroute.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "drivers")
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;

    @Column(name = "vehicle_type", nullable = false,
            columnDefinition = "VARCHAR(20) CHECK (vehicle_type IN ('sedan', 'van', 'wheelchair_van'))")
    private String vehicleType = "sedan";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "skills", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> skills = Map.of("wheelchair", false);

    @Column(name = "base_location", nullable = false)
    private String baseLocation = "Driver Base, 123 Main St";

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "shift_start")
    private LocalDateTime shiftStart;

    @Column(name = "shift_end")
    private LocalDateTime shiftEnd;

    @Column(name = "max_daily_rides")
    private Integer maxDailyRides = 8;
}