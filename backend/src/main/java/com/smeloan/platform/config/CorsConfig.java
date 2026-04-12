package com.smeloan.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * CORS (Cross-Origin Resource Sharing) configuration.
 *
 * <p>Allows the React front-end development servers running on
 * {@code localhost:3000} (customer portal) and {@code localhost:3001}
 * (staff portal) to call the API.</p>
 *
 * <p>For production, replace the allowed-origins list with your actual
 * front-end domain(s).</p>
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Allowed origins (development front-ends)
        config.setAllowedOrigins(List.of(
            "http://localhost:3000",
            "http://localhost:3001"
        ));

        // Allow common HTTP methods
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Allow standard and custom headers
        config.setAllowedHeaders(List.of(
            "Authorization",
            "Content-Type",
            "Accept",
            "X-Requested-With",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        ));

        // Expose response headers to the browser
        config.setExposedHeaders(List.of(
            "Authorization",
            "Content-Disposition"
        ));

        // Allow cookies / credentials to be included
        config.setAllowCredentials(true);

        // Cache the pre-flight response for 1 hour
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);

        return new CorsFilter(source);
    }
}
