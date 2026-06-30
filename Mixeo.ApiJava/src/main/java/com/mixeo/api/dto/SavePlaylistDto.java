package com.mixeo.api.dto;

import java.util.ArrayList;
import java.util.List;

/** Corps de /api/playlists/save (equivalent C# SavePlaylistDto). */
public class SavePlaylistDto {
    private String name;
    private List<Integer> mp3Ids = new ArrayList<>();
    private Integer userId;
    private PlaylistCriteriaDto criteria;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<Integer> getMp3Ids() { return mp3Ids; }
    public void setMp3Ids(List<Integer> mp3Ids) { this.mp3Ids = mp3Ids; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public PlaylistCriteriaDto getCriteria() { return criteria; }
    public void setCriteria(PlaylistCriteriaDto criteria) { this.criteria = criteria; }
}
