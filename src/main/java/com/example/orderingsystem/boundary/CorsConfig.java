package com.example.orderingsystem.boundary;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Mengizinkan frontend yang dibuka dari luar backend (file langsung, Live Server, preview IntelliJ)
 * memanggil API. Untuk produksi, ganti "*" dengan alamat frontend yang sebenarnya.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST")
                .allowedHeaders("*");
    }
}
