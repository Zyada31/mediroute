package com.mediroute.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mediroute")
public class RideConfig {
    private int depotDistance = 10;
    private int mphFactor = 120;

    public int getDepotDistance() {
        return depotDistance;
    }

    public void setDepotDistance(int depotDistance) {
        this.depotDistance = depotDistance;
    }

    public int getMphFactor() {
        return mphFactor;
    }

    public void setMphFactor(int mphFactor) {
        this.mphFactor = mphFactor;
    }
}