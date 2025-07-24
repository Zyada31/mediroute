package com.mediroute.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mediroute")
public class RideConfig {
    private int depotDistance = 10;
    private int mphFactor = 120;
}