package com.mediroute.dto;

import lombok.Data;

@Data
public class InviteCreateRes {
    private String inviteUrl; // one-time link sent via email/SMS
    public InviteCreateRes(String url) { this.inviteUrl = url; }
}