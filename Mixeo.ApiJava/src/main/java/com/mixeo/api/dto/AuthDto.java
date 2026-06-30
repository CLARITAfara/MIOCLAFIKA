package com.mixeo.api.dto;

/** Corps des requetes /api/auth/register et /api/auth/login. */
public class AuthDto {
    private String username;
    private String password;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
