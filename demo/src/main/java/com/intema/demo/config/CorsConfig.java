package com.intema.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // Origine del frontend
        // L'API usa Bearer token, non cookie; il frontend puo' essere servito da un host diverso.
        config.setAllowedOriginPatterns(java.util.List.of("*"));
        config.setAllowCredentials(false);
        // Metodi permessi
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // Header che accetti in input
        config.setAllowedHeaders(Arrays.asList("Origin", "Content-Type", "Accept", "Authorization"));
        // Header che vuoi esporre al client (per JWT ecc.)
        config.setExposedHeaders(Arrays.asList("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Applica a tutte le tue API
        source.registerCorsConfiguration("/api/**", config);

        return new CorsFilter(source);
    }
}
