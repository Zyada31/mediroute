package com.mediroute.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mediroute.dto.MobilityLevel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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
@ToString(exclude = {"rides", "history"}) // Prevent circular references
@EqualsAndHashCode(exclude = {"rides", "history"}) // Prevent circular references
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_id")
    private Long orgId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "phone", nullable = false)
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "emergency_contact_name")
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone")
    private String emergencyContactPhone;

    // Default Pickup Location - Using embedded Location
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "address", column = @Column(name = "default_pickup_address")),
            @AttributeOverride(name = "latitude", column = @Column(name = "default_pickup_lat")),
            @AttributeOverride(name = "longitude", column = @Column(name = "default_pickup_lng"))
    })
    private com.mediroute.entity.embeddable.Location defaultPickupLocation;

    // Default Dropoff Location - Using embedded Location
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "address", column = @Column(name = "default_dropoff_address")),
            @AttributeOverride(name = "latitude", column = @Column(name = "default_dropoff_lat")),
            @AttributeOverride(name = "longitude", column = @Column(name = "default_dropoff_lng"))
    })
    private com.mediroute.entity.embeddable.Location defaultDropoffLocation;

    // Medical Requirements
    @Column(name = "requires_wheelchair", columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    private Boolean requiresWheelchair = false;

    @Column(name = "requires_stretcher", columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    private Boolean requiresStretcher = false;

    @Column(name = "requires_oxygen", columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    private Boolean requiresOxygen = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "mobility_level")
    private MobilityLevel mobilityLevel;

    // Medical Information
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "medical_conditions", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> medicalConditions = new ArrayList<>();

    // Insurance Information
    @Column(name = "insurance_provider")
    private String insuranceProvider;

    @Column(name = "insurance_id")
    private String insuranceId;

    @Column(name = "medicaid_number")
    private String medicaidNumber;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    // Special Needs - Store as JSONB
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "special_needs", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> specialNeeds = new HashMap<>();

    // Patient Notes - SEPARATE FIELD (not conflicting with special_needs)
    @Column(name = "patient_notes", columnDefinition = "TEXT")
    private String patientNotes;

    @Column(name = "is_active", columnDefinition = "BOOLEAN DEFAULT TRUE")
    @Builder.Default
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationships - LAZY loading with proper configuration
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore  // Prevent JSON serialization issues
    @BatchSize(size = 10)  // Optimize batch loading
    @Builder.Default
    private List<Ride> rides = new ArrayList<>();

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    @BatchSize(size = 10)
    @Builder.Default
    private List<PatientHistory> history = new ArrayList<>();

    // ========== Business Methods ==========

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

    public String getRequiredVehicleType() {
        if (Boolean.TRUE.equals(requiresStretcher)) {
            return "STRETCHER_VAN";
        }
        if (Boolean.TRUE.equals(requiresWheelchair)) {
            return "WHEELCHAIR_VAN";
        }
        if (Boolean.TRUE.equals(requiresOxygen)) {
            return "WHEELCHAIR_VAN";
        }
        return "SEDAN";
    }

    // Helper method to get notes from special needs if stored there
    public String getNotes() {
        if (patientNotes != null && !patientNotes.isEmpty()) {
            return patientNotes;
        }
        // Check if notes exist in special needs for backward compatibility
        if (specialNeeds != null && specialNeeds.containsKey("notes")) {
            return String.valueOf(specialNeeds.get("notes"));
        }
        return null;
    }

    public void setNotes(String notes) {
        this.patientNotes = notes;
        // Also store in special needs for backward compatibility if needed
        if (notes != null && !notes.isEmpty()) {
            addSpecialNeed("notes", notes);
        }
    }
}