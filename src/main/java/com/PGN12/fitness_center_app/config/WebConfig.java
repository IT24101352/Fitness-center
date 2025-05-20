package com.PGN12.fitness_center_app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration class for enabling Cross-Origin Resource Sharing (CORS).
 * This allows your frontend (running on a different port/domain) to communicate
 * with the backend API.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**") // Apply CORS to all endpoints under /api/
                .allowedOrigins("*")   // Allow requests from any origin.
                // For production, restrict this to your frontend's domain:
                // .allowedOrigins("http://localhost:your_frontend_port", "https://yourfrontenddomain.com")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Allowed HTTP methods
                .allowedHeaders("*")   // Allow all headers
                .allowCredentials(false); // Set to true if you need to handle cookies/sessions with credentials
    }
}
