package com.hospital.hms.auth.dto;

import com.hospital.hms.common.enums.UserRole;

public class UserSummaryDTO {
    private Long id;
    private String username;
    private String email;
    private UserRole role;

    public UserSummaryDTO(Long id, String username, String email, UserRole role) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public UserRole getRole() { return role; }
}
