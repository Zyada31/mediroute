package com.mediroute.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

import java.util.Map;

// Option A: Add a configuration to serve a default favicon
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public SimpleUrlHandlerMapping faviconHandlerMapping() {
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setOrder(Integer.MIN_VALUE);
        mapping.setUrlMap(Map.of("/favicon.ico",
                new org.springframework.web.HttpRequestHandler() {
                    @Override
                    public void handleRequest(HttpServletRequest request, HttpServletResponse response) {
                        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                    }
                }));
        return mapping;
    }

    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager cacheManager() {
        // Fallback in-memory cache if Redis isn't configured/running
        return new ConcurrentMapCacheManager("osrm:distance");
    }
}
