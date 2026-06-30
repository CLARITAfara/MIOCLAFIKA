package com.mixeo.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

/**
 * Equivalent de Mixeo.Api.Models.PlaylistTrack (C#).
 * Table de liaison "playlist_tracks".
 */
@Entity
@Table(name = "playlist_tracks")
public class PlaylistTrack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "playlist_id")
    @JsonProperty("playlistId")
    private Integer playlistId;

    @Column(name = "mp3_id")
    @JsonProperty("mp3Id")
    private Integer mp3Id;

    // Relation N-1 vers Playlist : ignoree dans le JSON (evite le cycle).
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "playlist_id", insertable = false, updatable = false)
    private Playlist playlist;

    // Relation N-1 vers Mp3File : serialisee sous la cle "mp3File".
    @JsonProperty("mp3File")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "mp3_id", insertable = false, updatable = false)
    private Mp3File mp3File;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getPlaylistId() { return playlistId; }
    public void setPlaylistId(Integer playlistId) { this.playlistId = playlistId; }

    public Integer getMp3Id() { return mp3Id; }
    public void setMp3Id(Integer mp3Id) { this.mp3Id = mp3Id; }

    public Playlist getPlaylist() { return playlist; }
    public void setPlaylist(Playlist playlist) { this.playlist = playlist; }

    public Mp3File getMp3File() { return mp3File; }
    public void setMp3File(Mp3File mp3File) { this.mp3File = mp3File; }
}
