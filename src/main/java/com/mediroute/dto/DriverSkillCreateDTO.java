package com.mediroute.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Driver skill creation request")
public class DriverSkillCreateDTO {

    @NotBlank(message = "Skill name is required")
    @Schema(description = "Skill name", required = true)
    private String skillName;

    @Schema(description = "Skill level (1-5)", example = "3")
    private Integer skillLevel;

    @Schema(description = "Is skill certified")
    private Boolean isCertified = false;

    @Schema(description = "Certification date")
    private LocalDate certificationDate;

    @Schema(description = "Certification expiry date")
    private LocalDate expiryDate;

    @Schema(description = "Certifying organization")
    private String certifyingBody;
}
