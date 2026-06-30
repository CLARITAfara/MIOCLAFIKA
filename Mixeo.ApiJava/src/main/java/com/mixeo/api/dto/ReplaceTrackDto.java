package com.mixeo.api.dto;

/** Corps de /api/playlists/{id}/replace-track. */
public class ReplaceTrackDto {
    private int oldTrackId;
    private int newTrackId;

    public int getOldTrackId() { return oldTrackId; }
    public void setOldTrackId(int oldTrackId) { this.oldTrackId = oldTrackId; }

    public int getNewTrackId() { return newTrackId; }
    public void setNewTrackId(int newTrackId) { this.newTrackId = newTrackId; }
}
