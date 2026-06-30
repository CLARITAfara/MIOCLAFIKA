package com.mixeo.api.repository;

import com.mixeo.api.model.Mp3File;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface Mp3FileRepository extends JpaRepository<Mp3File, Integer> {
    List<Mp3File> findAllByOrderByCreatedAtDesc();
}
