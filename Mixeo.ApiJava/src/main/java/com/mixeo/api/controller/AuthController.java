package com.mixeo.api.controller;

import com.mixeo.api.dto.AuthDto;
import com.mixeo.api.model.User;
import com.mixeo.api.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

/**
 * Equivalent de Mixeo.Api.Controllers.AuthController (C#).
 * Routes : /api/auth/register et /api/auth/login.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository users;

    public AuthController(UserRepository users) {
        this.users = users;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthDto dto) {
        if (isBlank(dto.getUsername()) || isBlank(dto.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username and password are required."));
        }

        if (users.existsByUsernameIgnoreCase(dto.getUsername())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username already taken."));
        }

        // Hash "simple" identique au C# : Base64 du mot de passe (contexte projet/apprentissage).
        String passwordHash = Base64.getEncoder()
                .encodeToString(dto.getPassword().getBytes(StandardCharsets.UTF_8));

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPasswordHash(passwordHash);
        users.save(user);

        return ResponseEntity.ok(Map.of("id", user.getId(), "username", user.getUsername()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthDto dto) {
        if (isBlank(dto.getUsername()) || isBlank(dto.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username and password are required."));
        }

        String passwordHash = Base64.getEncoder()
                .encodeToString(dto.getPassword().getBytes(StandardCharsets.UTF_8));

        Optional<User> user = users.findByUsernameAndPasswordHash(dto.getUsername(), passwordHash);
        if (user.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid credentials."));
        }

        return ResponseEntity.ok(Map.of("id", user.get().getId(), "username", user.get().getUsername()));
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
