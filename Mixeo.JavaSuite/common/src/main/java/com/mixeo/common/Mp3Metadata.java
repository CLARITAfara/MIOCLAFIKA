package com.mixeo.common;

/** Equivalent de Mixeo.Common.Mp3Metadata (C#). Serialise/deserialise en JSON sur les queues. */
public class Mp3Metadata {
    private String path = "";
    private String title = "";
    private String album = "";
    private String genre = "";
    private String artist = "";
    private int duration;
    private int year;

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAlbum() { return album; }
    public void setAlbum(String album) { this.album = album; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
}
