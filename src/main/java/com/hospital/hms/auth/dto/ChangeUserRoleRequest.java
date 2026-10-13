package com.hospital.hms.auth.dto;

import com.hospital.hms.common.enums.UserRole;
import jakarta.validation.constraints.NotNull;

public class ChangeUserRoleRequest {
    @NotNull(message = "Role is required")
    private UserRole role;

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
}
