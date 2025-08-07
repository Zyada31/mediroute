// ============================================================================
// CONFIGURATION LAYER
// ============================================================================

// 1. Swagger Configuration
package com.mediroute.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI mediRouteOpenAPI() {
        return new OpenAPI()
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development"),
                        new Server().url("https://api.mediroute.com").description("Production")
                ))
                .info(new Info()
                        .title("MediRoute API")
                        .description("Medical Transport Management System")
                        .version("v2.0")
                        .contact(new Contact()
                                .name("MediRoute Team")
                                .email("support@mediroute.com")
                                .url("https://mediroute.com")));
    }
}