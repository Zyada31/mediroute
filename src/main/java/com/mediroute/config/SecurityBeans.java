package com.mediroute.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
public class SecurityBeans {
    @Bean
    public PasswordEncoder passwordEncoder() {
        // supports {bcrypt}, {noop}, {pbkdf2}, etc.
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    public static Long currentOrgId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        Object details = auth.getDetails();
        if (details instanceof java.util.Map<?,?> map) {
            Object orgId = map.get("orgId");
            if (orgId instanceof Number n) return n.longValue();
            try { return orgId != null ? Long.valueOf(orgId.toString()) : null; } catch (Exception ignored) {}
        }
        return null;
    }
}