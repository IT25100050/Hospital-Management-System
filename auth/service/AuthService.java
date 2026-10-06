package com.hospital.hms.auth.service;

import com.hospital.hms.auth.dto.*;
import java.util.List;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    String register(RegisterRequest request);
    void resetPassword(PasswordResetRequest request);
    List<UserSummaryDTO> getAllUsers();
    void adminResetPassword(String username, String newPassword);
}
