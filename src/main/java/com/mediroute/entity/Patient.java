package com.mediroute.entity;

import com.mediroute.dto.MobilityLevel;
import com.mediroute.entity.embeddable.Location;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "patients", indexes = {
        @Index(name = "idx_patient_phone", columnList = "phone"),
        @Index(name = "idx_patient_name", columnList = "name"),
        @Index(name = "idx_patient_active", columnList = "is_active")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@Schema(description = "Patient entity for medical transport")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique patient identifier")
    private Long id;

    @Column(name = "name", nullable = false)
    @Schema(description = "Patient full name", required = true)
    private String name;

    // Contact Information
    @Column(name = "phone", nullable = false)
    @Schema(description = "Primary phone number", required = true)
    private String phone;

    @Column(name = "email")
    @Schema(description = "Email address")
    private String email;

    @Column(name = "emergency_contact_name")
    @Schema(description = "Emergency contact name")
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone")
    @Schema(description = "Emergency contact phone")
    private String emergencyContactPhone;

    // Default Locations
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "address", column = @Column(name = "default_pickup_address")),
            @AttributeOverride(name = "latitude", column = @Column(name = "default_pickup_lat")),
            @AttributeOverride(name = "longitude", column = @Column(name = "default_pickup_lng"))
    })
    private Location defaultPickupLocation;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "address", column = @Column(name = "default_dropoff_address")),
            @AttributeOverride(name = "latitude", column = @Column(name = "default_dropoff_lat")),
            @AttributeOverride(name = "longitude", column = @Column(name = "default_dropoff_lng"))
    })
    private Location defaultDropoffLocation;

    // Medical Requirements
    @Column(name = "requires_wheelchair", columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    @Schema(description = "Requires wheelchair accessible vehicle")
    private Boolean requiresWheelchair = false;

    @Column(name = "requires_stretcher", columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    @Schema(description = "Requires stretcher capable vehicle")
    private Boolean requiresStretcher = false;

    @Column(name = "requires_oxygen", columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    @Schema(description = "Requires oxygen equipped vehicle")
    private Boolean requiresOxygen = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "mobility_level")
    @Schema(description = "Patient mobility level")
    private MobilityLevel mobilityLevel;

    // Medical Information
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "medical_conditions", columnDefinition = "jsonb")
    @Builder.Default
    @Schema(description = "List of medical conditions")
    private List<String> medicalConditions = new ArrayList<>();

    // Insurance Information
    @Column(name = "insurance_provider")
    @Schema(description = "Insurance provider name")
    private String insuranceProvider;

    @Column(name = "insurance_id")
    @Schema(description = "Insurance ID number")
    private String insuranceId;

    @Column(name = "medicaid_number")
    @Schema(description = "Medicaid number")
    private String medicaidNumber;

    // Additional Fields
    @Column(name = "date_of_birth")
    @Schema(description = "Date of birth")
    private LocalDate dateOfBirth;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "special_needs", columnDefinition = "jsonb")
    @Builder.Default
    @Schema(description = "Special needs and requirements")
    private Map<String, Object> specialNeeds = new HashMap<>();

    // Status and Audit
    @Column(name = "is_active", columnDefinition = "BOOLEAN DEFAULT TRUE")
    @Builder.Default
    @Schema(description = "Whether patient is active")
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationships
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Ride> rides = new ArrayList<>();

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PatientHistory> history = new ArrayList<>();

    // Business Methods
    public void addMedicalCondition(String condition) {
        if (this.medicalConditions == null) {
            this.medicalConditions = new ArrayList<>();
        }
        if (!this.medicalConditions.contains(condition)) {
            this.medicalConditions.add(condition);
        }
    }

    public void addSpecialNeed(String key, Object value) {
        if (this.specialNeeds == null) {
            this.specialNeeds = new HashMap<>();
        }
        this.specialNeeds.put(key, value);
    }

    public boolean hasHighMobilityNeeds() {
        return Boolean.TRUE.equals(requiresWheelchair) ||
                Boolean.TRUE.equals(requiresStretcher) ||
                mobilityLevel == MobilityLevel.WHEELCHAIR ||
                mobilityLevel == MobilityLevel.STRETCHER;
    }

    public boolean requiresSpecialVehicle() {
        return Boolean.TRUE.equals(requiresWheelchair) ||
                Boolean.TRUE.equals(requiresStretcher) ||
                Boolean.TRUE.equals(requiresOxygen);
    }
}