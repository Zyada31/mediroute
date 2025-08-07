package com.mediroute.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "driver_skills",
        uniqueConstraints = @UniqueConstraint(columnNames = {"driver_id", "skill_name"}),
        indexes = {
                @Index(name = "idx_skill_driver", columnList = "driver_id"),
                @Index(name = "idx_skill_name", columnList = "skill_name"),
                @Index(name = "idx_skill_certified", columnList = "is_certified")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@Schema(description = "Driver skill certifications")
public class DriverSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique skill record identifier")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    @Schema(description = "Driver with this skill")
    private Driver driver;

    @Column(name = "skill_name", nullable = false)
    @Schema(description = "Name of the skill")
    private String skillName;

    @Column(name = "skill_level")
    @Schema(description = "Skill level (1-5)")
    private Integer skillLevel;

    @Column(name = "is_certified", columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    @Schema(description = "Whether skill is certified")
    private Boolean isCertified = false;

    @Column(name = "certification_date")
    @Schema(description = "Date of certification")
    private LocalDate certificationDate;

    @Column(name = "expiry_date")
    @Schema(description = "Certification expiry date")
    private LocalDate expiryDate;

    @Column(name = "certifying_body")
    @Schema(description = "Organization that provided certification")
    private String certifyingBody;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Business Methods
    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now());
    }

    public boolean isExpiringSoon(int daysAhead) {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now().plusDays(daysAhead));
    }
}
    