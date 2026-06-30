package com.mixeo.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

/**
 * Equivalent de Mixeo.Api.Models.PlaylistRule (C#).
 * Mappe la table "playlist_rules".
 */
@Entity
@Table(name = "playlist_rules")
public class PlaylistRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "playlist_id")
    @JsonProperty("playlistId")
    private Integer playlistId;

    @Column(name = "max_duration")
    @JsonProperty("maxDuration")
    private Integer maxDuration;

    @Column(name = "genre")
    private String genre;

    @Column(name = "artist")
    private String artist;

    @Column(name = "language")
    private String language;

    @Column(name = "exclude_artist")
    @JsonProperty("excludeArtist")
    private String excludeArtist;

    @Column(name = "exclude_genre")
    @JsonProperty("excludeGenre")
    private String excludeGenre;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "playlist_id", insertable = false, updatable = false)
    private Playlist playlist;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getPlaylistId() { return playlistId; }
    public void setPlaylistId(Integer playlistId) { this.playlistId = playlistId; }

    public Integer getMaxDuration() { return maxDuration; }
    public void setMaxDuration(Integer maxDuration) { this.maxDuration = maxDuration; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getExcludeArtist() { return excludeArtist; }
    public void setExcludeArtist(String excludeArtist) { this.excludeArtist = excludeArtist; }

    public String getExcludeGenre() { return excludeGenre; }
    public void setExcludeGenre(String excludeGenre) { this.excludeGenre = excludeGenre; }

    public Playlist getPlaylist() { return playlist; }
    public void setPlaylist(Playlist playlist) { this.playlist = playlist; }
}
