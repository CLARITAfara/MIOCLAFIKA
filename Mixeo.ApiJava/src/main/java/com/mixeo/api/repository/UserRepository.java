package com.mixeo.api.repository;

import com.mixeo.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    boolean existsByUsernameIgnoreCase(String username);
    Optional<User> findByUsernameAndPasswordHash(String username, String passwordHash);
}
