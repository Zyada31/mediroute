package com.mediroute.config;

import com.mediroute.service.security.JwtAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) -> {
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    res.setContentType("application/json");
                    res.getWriter().write("{\"error\":\"unauthorized\"}");
                })
                .accessDeniedHandler((req, res, e) -> {
                    res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    res.setContentType("application/json");
                    res.getWriter().write("{\"error\":\"forbidden\"}");
                })
            )
            .authorizeHttpRequests(auth -> auth
                // Public: health/docs/static
                .requestMatchers(
                    "/", "/index.html", "/public/**", "/static/**",
                    "/actuator/health", "/actuator/info",
                    "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"
                ).permitAll()

                // Public auth endpoints (if present)
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()

                // Admin/dispatcher features
                .requestMatchers("/api/v1/optimization/**").hasAnyRole("ADMIN","DISPATCHER")
                .requestMatchers("/api/v1/assign/**").hasAnyRole("ADMIN","DISPATCHER")
                .requestMatchers("/v1/users/**").hasAnyRole("ADMIN","DISPATCHER")

                // Provider uploads
                .requestMatchers(HttpMethod.POST, "/api/v1/rides/upload", "/api/v1/rides/upload-and-optimize")
                    .hasAnyRole("PROVIDER","ADMIN","DISPATCHER")

                // Read-only data endpoints for provider/dispatch/admin
                .requestMatchers(HttpMethod.GET, "/api/v1/rides/**", "/api/v1/statistics/**", "/api/v1/drivers/qualified")
                    .hasAnyRole("PROVIDER","ADMIN","DISPATCHER")

                // Everything else requires authentication
                .anyRequest().authenticated()
            );

        http.addFilterBefore(jwtAuthFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}

//// src/main/java/com/mediroute/config/SecurityConfig.java
//package com.mediroute.config;
//
//import com.mediroute.service.security.JwtAuthFilter;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.HttpMethod;
//import org.springframework.security.config.Customizer;
//import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.http.SessionCreationPolicy;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.web.cors.*;
//
//import java.util.List;
//
//@Configuration
//@EnableMethodSecurity // enables @PreAuthorize, etc.
//public class SecurityConfig {
//
//    private final JwtAuthFilter jwtAuthFilter;
//
//    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
//        this.jwtAuthFilter = jwtAuthFilter;
//    }
//
//    // CORS for SPA
//    @Bean
//    public CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration cors = new CorsConfiguration();
//        cors.setAllowedOrigins(List.of(
//                "http://localhost:5173",     // Vite dev
//                "http://localhost:3000",     // other local
//                "https://app.mediroute.com"  // prod SPA
//        ));
//        cors.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
//        cors.setAllowedHeaders(List.of("*"));
//        cors.setAllowCredentials(true);
//        cors.setMaxAge(3600L);
//
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", cors);
//        return source;
//    }
//
//    @Bean
//    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//        http
//                .cors(Customizer.withDefaults())
//                .csrf(csrf -> csrf.disable())
//                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//                .exceptionHandling(ex -> ex
//                        .authenticationEntryPoint((req, res, e) -> {
//                            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//                            res.setContentType("application/json");
//                            res.getWriter().write("{\"error\":\"unauthorized\"}");
//                        })
//                        .accessDeniedHandler((req, res, e) -> {
//                            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
//                            res.setContentType("application/json");
//                            res.getWriter().write("{\"error\":\"forbidden\"}");
//                        })
//                )
//                .authorizeHttpRequests(auth -> auth
//                        // Swagger / health / dev token
//                        .requestMatchers(
//                                "/actuator/health", "/actuator/info",
//                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
//                                "/api/dev/token" ,        // dev-only helper
//                                "/api/v1/auth/login"         // dev-only helper
//                        ).permitAll()
//// in authorizeHttpRequests()
//                                .requestMatchers(HttpMethod.POST, "/v1/users/invite").hasAnyRole("ADMIN","DISPATCHER") // or ADMIN only
//                                .requestMatchers(HttpMethod.POST, "/v1/auth/activate").permitAll()
//                                // SecurityConfig.authorizeHttpRequests(...)
//                                .requestMatchers(HttpMethod.POST, "/v1/auth/mfa/totp/setup", "/v1/auth/mfa/totp/verify").authenticated()
//                        // Public auth endpoints (if/when you add them)
//                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/refresh").permitAll()
//
//                        // Evidence upload (driver action; admins/dispatchers allowed too)
//                        .requestMatchers(HttpMethod.POST, "/api/v1/rides/*/evidence")
//                        .hasAnyRole("DRIVER","ADMIN","DISPATCHER")
//
//                        // Ride status updates (driver & dispatch/admin)
//                        .requestMatchers(HttpMethod.POST, "/api/v1/rides/*/status")
//                        .hasAnyRole("DRIVER","ADMIN","DISPATCHER")
//
//                        // Admin/dispatcher features
//                        .requestMatchers("/api/v1/optimization/**").hasAnyRole("ADMIN","DISPATCHER")
//                        .requestMatchers("/api/v1/assign/**").hasAnyRole("ADMIN","DISPATCHER")
//                        .requestMatchers("/api/v1/drivers/**").hasAnyRole("ADMIN","DISPATCHER") // (see method-level overrides below)
//                        .requestMatchers("/api/v1/statistics/**").hasAnyRole("ADMIN","DISPATCHER")
//
//                        // Provider: uploads & basic summary
//                        .requestMatchers(HttpMethod.POST, "/api/v1/rides/upload", "/api/v1/rides/upload-and-optimize")
//                        .hasAnyRole("PROVIDER","ADMIN","DISPATCHER")
//                        .requestMatchers(HttpMethod.GET, "/api/v1/rides/date/**", "/api/v1/rides/unassigned")
//                        .hasAnyRole("PROVIDER","ADMIN","DISPATCHER")
//
//                        // Everything else needs auth
//                        .anyRequest().authenticated()
//                );
//
//
//
//        // Put your JWT filter before the default auth filter
//        http.addFilterBefore(jwtAuthFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
//        return http.build();
//    }
//}