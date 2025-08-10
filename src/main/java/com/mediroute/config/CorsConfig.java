package com.mediroute.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins(
                                "http://localhost:5173",   // 👈 Vite dev
                                "http://127.0.0.1:5173",   // 👈 sometimes needed
                                "http://localhost:3000",
                                "http://localhost:8080",
                                "https://app.mediroute.com"
                        )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .maxAge(3600);
            }
        };
    }
//    @Bean
//    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http
//                .csrf(csrf -> csrf.disable()) // dev only
//                .cors(cors -> {})             // 👈 enable CORS
//                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers("/api/**").permitAll() // dev only; tighten later
//                        .anyRequest().permitAll()
//                );
//        return http.build();
//    }
}
