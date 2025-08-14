// src/main/java/com/mediroute/config/AppProps.java
package com.mediroute.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProps {

    private Security security = new Security();
    private Jwt jwt = new Jwt();
    private Optimizer optimizer = new Optimizer();

    public Security getSecurity() { return security; }
    public Jwt getJwt() { return jwt; }
    public Optimizer getOptimizer() { return optimizer; }

    public static class Security {
        private String issuer = "https://mediroute.local";
        private int accessTokenTtlMin = 10;
        private int refreshTokenTtlDays = 14;
        private String cookieName = "refresh_token";
        private Cors cors = new Cors();

        public String getIssuer() { return issuer; }
        public void setIssuer(String issuer) { this.issuer = issuer; }
        public int getAccessTokenTtlMin() { return accessTokenTtlMin; }
        public void setAccessTokenTtlMin(int accessTokenTtlMin) { this.accessTokenTtlMin = accessTokenTtlMin; }
        public int getRefreshTokenTtlDays() { return refreshTokenTtlDays; }
        public void setRefreshTokenTtlDays(int refreshTokenTtlDays) { this.refreshTokenTtlDays = refreshTokenTtlDays; }
        public String getCookieName() { return cookieName; }
        public void setCookieName(String cookieName) { this.cookieName = cookieName; }
        public Cors getCors() { return cors; }
    }

    public static class Cors {
        private List<String> origins = List.of("http://localhost:5173");

        public List<String> getOrigins() { return origins; }
        public void setOrigins(List<String> origins) { this.origins = origins; }
    }

    public static class Jwt {
        /** PEM strings (or load them from a secret store) */
        private String privateKeyPem;
        private String publicKeyPem;

        public String getPrivateKeyPem() { return privateKeyPem; }
        public void setPrivateKeyPem(String privateKeyPem) { this.privateKeyPem = privateKeyPem; }
        public String getPublicKeyPem() { return publicKeyPem; }
        public void setPublicKeyPem(String publicKeyPem) { this.publicKeyPem = publicKeyPem; }
    }

    public static class Optimizer {
        /** If true, run a relaxed second pass for unassigned rides. */
        private boolean relaxForUnassigned = false;
        /** Cap additional relaxed assignments per driver in second pass. */
        private int relaxMaxPerDriver = 2;
        /** Max pickup distance (km) in normal pass. */
        private double maxPickupDistanceKm = 50.0;

        public boolean isRelaxForUnassigned() { return relaxForUnassigned; }
        public void setRelaxForUnassigned(boolean relaxForUnassigned) { this.relaxForUnassigned = relaxForUnassigned; }
        public int getRelaxMaxPerDriver() { return relaxMaxPerDriver; }
        public void setRelaxMaxPerDriver(int relaxMaxPerDriver) { this.relaxMaxPerDriver = relaxMaxPerDriver; }
        public double getMaxPickupDistanceKm() { return maxPickupDistanceKm; }
        public void setMaxPickupDistanceKm(double v) { this.maxPickupDistanceKm = v; }
    }
}