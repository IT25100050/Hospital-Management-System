package com.hospital.hms.auth.dto;

public class CurrentUserDTO {
    private final String username;
    private final String role;

    public CurrentUserDTO(String username, String role) {
        this.username = username;
        this.role = role;
    }

    public String getUsername() { return username; }
    public String getRole() { return role; }
}
