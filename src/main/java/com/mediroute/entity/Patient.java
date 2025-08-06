package com.mediroute.entity;

import com.mediroute.dto.MobilityLevel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "patients")
@Entity
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "contact_info")
    private String contactInfo;

    // NEW: Enhanced contact information
    @Column(name = "phone")
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "emergency_contact_name")
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone")
    private String emergencyContactPhone;

    @Column(name = "default_pickup_location")
    private String defaultPickupLocation;

    @Column(name = "default_dropoff_location")
    private String defaultDropoffLocation;

    // Your existing field
    @Column(name = "requires_wheelchair", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean requiresWheelchair;

    // NEW: Additional medical requirements
    @Column(name = "requires_stretcher", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean requiresStretcher;

    @Column(name = "requires_oxygen", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean requiresOxygen;

    @Enumerated(EnumType.STRING)
    @Column(name = "mobility_level")
    private MobilityLevel mobilityLevel;

    // NEW: Medical information
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "medical_conditions", columnDefinition = "jsonb")
    private List<String> medicalConditions;

    @Column(name = "insurance_provider")
    private String insuranceProvider;

    @Column(name = "insurance_id")
    private String insuranceId;

    @Column(name = "medicaid_number")
    private String medicaidNumber;

    @Column(name = "is_active", columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean isActive;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    // Your existing field
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "special_needs", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> specialNeeds = new HashMap<>();

    // Constructors
    public Patient(String name, String contactInfo) {
        this.name = name;
        this.contactInfo = contactInfo;
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
    }

    // Your existing method
    public void addSpecialNeed(String key, Object value) {
        if (this.specialNeeds == null) {
            this.specialNeeds = new HashMap<>();
        }
        this.specialNeeds.put(key, value);
    }

    // NEW: Medical transport helper methods
    public void addMedicalCondition(String condition) {
        if (this.medicalConditions == null) {
            this.medicalConditions = new ArrayList<>();
        }
        this.medicalConditions.add(condition);
    }

    public boolean hasHighMobilityNeeds() {
        return Boolean.TRUE.equals(requiresWheelchair) ||
                Boolean.TRUE.equals(requiresStretcher) ||
                mobilityLevel == MobilityLevel.WHEELCHAIR ||
                mobilityLevel == MobilityLevel.STRETCHER;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}