package com.hospital.hms.auth.service;

import com.hospital.hms.auth.dto.*;
import com.hospital.hms.common.enums.UserRole;
import java.util.List;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    String register(RegisterRequest request);
    void resetPassword(PasswordResetRequest request, String authenticatedUsername);
    List<UserSummaryDTO> getAllUsers();
    UserSummaryDTO adminCreateUser(AdminCreateUserRequest request);
    void adminDeleteUser(String username, String authenticatedAdminUsername);
    UserSummaryDTO updateUserRole(String username, UserRole role);
    void adminResetPassword(String username, String newPassword);
}
