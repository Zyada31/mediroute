package com.mediroute.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Entity
@Table(name = "ride_audit", indexes = {
        @Index(name = "idx_audit_ride", columnList = "ride_id"),
        @Index(name = "idx_audit_time", columnList = "changed_at"),
        @Index(name = "idx_audit_field", columnList = "field_name")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@Schema(description = "Audit trail for ride changes")
public class RideAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique audit record identifier")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id", nullable = false)
    @Schema(description = "Ride that was changed")
    private Ride ride;

    @Column(name = "field_name", nullable = false)
    @Schema(description = "Name of the field that changed")
    private String fieldName;

    @Column(name = "old_value", columnDefinition = "TEXT")
    @Schema(description = "Previous value")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    @Schema(description = "New value")
    private String newValue;

    @Column(name = "changed_by", nullable = false)
    @Schema(description = "Who made the change")
    private String changedBy;

    @CreatedDate
    @Column(name = "changed_at", updatable = false)
    @Schema(description = "When the change occurred")
    private LocalDateTime changedAt;

    @Column(name = "change_reason")
    @Schema(description = "Reason for the change")
    private String changeReason;

    @Column(name = "system_generated", columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    @Schema(description = "Whether change was system generated")
    private Boolean systemGenerated = false;
}