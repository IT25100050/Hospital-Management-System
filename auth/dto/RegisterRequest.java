package com.hospital.hms.auth.dto;

import com.hospital.hms.common.enums.UserRole;

public class RegisterRequest {
    private String username;
    private String email;
    private String password;
    private UserRole role;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
}
