package com.mixeo.api.dto;

import java.util.ArrayList;
import java.util.List;

/** Criteres de generation d'une playlist (equivalent C# PlaylistCriteriaDto). */
public class PlaylistCriteriaDto {
    private String name = "Nouvelle Playlist";
    private int totalDuration; // en secondes
    private List<String> genres = new ArrayList<>();
    private List<String> languages = new ArrayList<>();
    private List<String> artists = new ArrayList<>();
    private List<String> albums = new ArrayList<>();
    private List<String> excludeArtists = new ArrayList<>();
    private List<String> excludeGenres = new ArrayList<>();
    private List<String> excludeAlbums = new ArrayList<>();

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getTotalDuration() { return totalDuration; }
    public void setTotalDuration(int totalDuration) { this.totalDuration = totalDuration; }

    public List<String> getGenres() { return genres; }
    public void setGenres(List<String> genres) { this.genres = genres; }

    public List<String> getLanguages() { return languages; }
    public void setLanguages(List<String> languages) { this.languages = languages; }

    public List<String> getArtists() { return artists; }
    public void setArtists(List<String> artists) { this.artists = artists; }

    public List<String> getAlbums() { return albums; }
    public void setAlbums(List<String> albums) { this.albums = albums; }

    public List<String> getExcludeArtists() { return excludeArtists; }
    public void setExcludeArtists(List<String> excludeArtists) { this.excludeArtists = excludeArtists; }

    public List<String> getExcludeGenres() { return excludeGenres; }
    public void setExcludeGenres(List<String> excludeGenres) { this.excludeGenres = excludeGenres; }

    public List<String> getExcludeAlbums() { return excludeAlbums; }
    public void setExcludeAlbums(List<String> excludeAlbums) { this.excludeAlbums = excludeAlbums; }
}
