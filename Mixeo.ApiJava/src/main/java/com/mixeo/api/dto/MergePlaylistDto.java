package com.mixeo.api.dto;

import java.util.List;

public class MergePlaylistDto {
    private List<Integer> playlistIds;
    private String newName;
    private Integer userId;

    public List<Integer> getPlaylistIds() { return playlistIds; }
    public void setPlaylistIds(List<Integer> playlistIds) { this.playlistIds = playlistIds; }

    public String getNewName() { return newName; }
    public void setNewName(String newName) { this.newName = newName; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
}
