package com.hospital.hms.auth.dto;

import com.hospital.hms.common.enums.UserRole;

public class LoginResponse {
    private String token;
    private String username;
    private UserRole role;

    public LoginResponse(String token, String username, UserRole role) {
        this.token = token;
        this.username = username;
        this.role = role;
    }

    public String getToken() { return token; }
    public String getUsername() { return username; }
    public UserRole getRole() { return role; }
}
