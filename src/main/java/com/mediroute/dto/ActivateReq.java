package com.mediroute.dto;

import lombok.Data;

@Data
public class ActivateReq {
    private String token;      // one-time invite token from URL
    private String password;   // or null if using passkey first
    private String totp;       // optional at activation time
}
