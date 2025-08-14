// src/main/java/com/mediroute/service/security/JwtAuthFilter.java
package com.mediroute.service.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.*;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwt;

    public JwtAuthFilter(JwtService jwt) { this.jwt = jwt; }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String auth = req.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            try {
                Map<String,Object> claims = jwt.parseAndValidate(token);

                List<SimpleGrantedAuthority> authorities = List.of();
                Object rolesClaim = claims.get("roles");
                if (rolesClaim instanceof List<?> list) {
                    authorities = list.stream()
                            .map(Object::toString)
                            .map(this::normalizeRole)
                            .filter(s -> !s.isBlank())
                            .map(s -> new SimpleGrantedAuthority("ROLE_" + s))
                            .toList();
                } else if (rolesClaim instanceof String s) {
                    // Try to handle CSV or JSON array stored as string
                    String cleaned = s.replace('[',' ').replace(']',' ').replace('"',' ');
                    authorities = java.util.Arrays.stream(cleaned.split(","))
                            .map(String::trim)
                            .map(this::normalizeRole)
                            .filter(str -> !str.isBlank())
                            .map(str -> new SimpleGrantedAuthority("ROLE_" + str))
                            .toList();
                }

                Object subObj = claims.getOrDefault("sub", "user");
                String subject = String.valueOf(subObj);
                var authentication =
                        new UsernamePasswordAuthenticationToken(subject, null, authorities);

                // attach driverId if present (for downstream controllers)
                Object driverId = claims.get("driverId");
                Object orgId = claims.get("orgId");
                if (driverId != null) {
                    Map<String,Object> details = new HashMap<>();
                    details.put("driverId", driverId);
                    if (orgId != null) details.put("orgId", orgId);
                    authentication.setDetails(details);
                } else if (orgId != null) {
                    Map<String,Object> details = new HashMap<>();
                    details.put("orgId", orgId);
                    authentication.setDetails(details);
                }

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                // invalid token: let it pass as anonymous; optionally short-circuit with 401
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(req, res);
    }

    private String normalizeRole(String raw) {
        if (raw == null) return "";
        String r = raw.trim();
        // Strip quotes/brackets and keep uppercase letters and underscore
        r = r.replace("\"", "").replace("'", "").replace("[", "").replace("]", "");
        r = r.toUpperCase();
        r = r.replaceAll("[^A-Z_]", "");
        return r;
    }
}