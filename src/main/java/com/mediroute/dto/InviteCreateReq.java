// src/main/java/com/mediroute/dto/InviteDTOs.java
package com.mediroute.dto;

import lombok.Data;

@Data
public class InviteCreateReq {
    private String email;
    private String role;   // ADMIN, DISPATCHER, DRIVER, PROVIDER
    private Long orgId;
    private Long driverId; // optional for DRIVER
}


