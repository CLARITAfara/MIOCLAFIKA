package com.mixeo.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Equivalent du bloc CORS + UseStaticFiles du Program.cs C#.
 * - CORS ouvert (AllowAnyOrigin / Method / Header) avec exposition des headers Range.
 * - Le dossier "Uploads" est expose en statique sous /Uploads.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${mixeo.uploads-dir:Uploads}")
    private String uploadsDir;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("*")
                .allowedHeaders("*")
                .exposedHeaders("Content-Range", "Accept-Ranges", "Content-Length");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolute = Paths.get(uploadsDir).toAbsolutePath().toString();
        registry.addResourceHandler("/Uploads/**")
                .addResourceLocations("file:" + absolute + "/");
    }
}
