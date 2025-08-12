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

                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) claims.getOrDefault("roles", List.of());
                var authorities = roles.stream()
                        .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                        .toList();

                String subject = (String) claims.getOrDefault("sub", "user");
                var authentication =
                        new UsernamePasswordAuthenticationToken(subject, null, authorities);

                // attach driverId if present (for downstream controllers)
                Object driverId = claims.get("driverId");
                if (driverId != null) {
                    Map<String,Object> details = new HashMap<>();
                    details.put("driverId", driverId);
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
}