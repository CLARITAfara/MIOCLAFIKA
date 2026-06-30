package com.mixeo.api.repository;

import com.mixeo.api.model.PlaylistTrack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlaylistTrackRepository extends JpaRepository<PlaylistTrack, Integer> {
    List<PlaylistTrack> findByPlaylistId(Integer playlistId);
    Optional<PlaylistTrack> findFirstByPlaylistIdAndMp3Id(Integer playlistId, Integer mp3Id);
}
