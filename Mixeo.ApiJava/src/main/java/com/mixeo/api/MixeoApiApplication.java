package com.mixeo.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entree de l'API Mixeo (port Java/Spring Boot de Mixeo.Api).
 * Equivalent du Program.cs en C#.
 */
@SpringBootApplication
public class MixeoApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(MixeoApiApplication.class, args);
    }
}
