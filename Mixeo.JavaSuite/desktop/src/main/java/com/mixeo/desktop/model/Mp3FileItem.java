package com.mixeo.desktop.model;

/**
 * Equivalent de Mixeo.Desktop.Models.Mp3File (C#).
 * Les champs metadata sont remplis a l'affichage pour la grille JavaFX.
 */
public class Mp3FileItem {
    private String title = "";
    private String artist = "";
    private String album = "";
    private String genre = "";
    private int year;
    private int duration;
    private String absolutePath = "";

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public String getAlbum() { return album; }
    public void setAlbum(String album) { this.album = album; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public String getAbsolutePath() { return absolutePath; }
    public void setAbsolutePath(String absolutePath) { this.absolutePath = absolutePath; }
}
