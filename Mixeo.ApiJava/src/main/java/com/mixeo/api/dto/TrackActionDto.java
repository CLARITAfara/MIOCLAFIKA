package com.mixeo.api.dto;

/** Corps de /api/playlists/{id}/add-track et /remove-track. */
public class TrackActionDto {
    private int trackId;

    public int getTrackId() { return trackId; }
    public void setTrackId(int trackId) { this.trackId = trackId; }
}
