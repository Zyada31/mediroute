package com.mediroute.config;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.mediroute.config.jackson.LocalTimeFlexDeserializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalTime;

@Configuration
public class JacksonConfig {

    /**
     * Customize the auto-configured ObjectMapper without replacing Spring Boot's defaults
     * (which include JavaTimeModule for java.time types).
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> builder.postConfigurer(objectMapper -> {
            SimpleModule timeModule = new SimpleModule();
            timeModule.addDeserializer(LocalTime.class, new LocalTimeFlexDeserializer());
            objectMapper.registerModule(timeModule);
        });
    }
}


