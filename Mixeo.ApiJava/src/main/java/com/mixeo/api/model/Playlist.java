package com.mixeo.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Equivalent de Mixeo.Api.Models.Playlist (C#).
 * Mappe la table "playlists".
 */
@Entity
@Table(name = "playlists")
public class Playlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name")
    private String name;

    @Column(name = "total_duration")
    private int totalDuration;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Relation N-1 vers User. Ignoree dans le JSON (comme [JsonIgnore] en C#).
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    // Relation 1-N vers les pistes. Serialisee sous la cle "tracks".
    @OneToMany(mappedBy = "playlist", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlaylistTrack> tracks = new ArrayList<>();

    // Relation 1-1 vers la regle optionnelle. Serialisee sous la cle "rule".
    @OneToOne(mappedBy = "playlist", cascade = CascadeType.ALL, orphanRemoval = true)
    private PlaylistRule rule;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getTotalDuration() { return totalDuration; }
    public void setTotalDuration(int totalDuration) { this.totalDuration = totalDuration; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public List<PlaylistTrack> getTracks() { return tracks; }
    public void setTracks(List<PlaylistTrack> tracks) { this.tracks = tracks; }

    public PlaylistRule getRule() { return rule; }
    public void setRule(PlaylistRule rule) { this.rule = rule; }
}
