package com.hospital.hms.auth.controller;

import com.hospital.hms.auth.dto.*;
import com.hospital.hms.auth.service.AuthService;
import com.hospital.hms.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Login successful", response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CurrentUserDTO>> getCurrentUser(Authentication authentication) {
        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Authenticated account has no assigned role."));
        return ResponseEntity.ok(new ApiResponse<>(true, "Current user retrieved successfully",
                new CurrentUserDTO(authentication.getName(), role)));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody RegisterRequest request) {
        String response = authService.register(request);
        return ResponseEntity.ok(new ApiResponse<>(true, response, null));
    }

    /**
     * Self-service: a logged-in user changes their own password, proving they know the current one.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody PasswordResetRequest request,
                                                            Authentication authentication) {
        authService.resetPassword(request, authentication.getName());
        return ResponseEntity.ok(new ApiResponse<>(true, "Password reset successfully", null));
    }

    /**
     * Admin-only: list registered users so an admin can pick who needs a password reset.
     * No password data is ever included in the response.
     */
    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserSummaryDTO>>> getAllUsers() {
        List<UserSummaryDTO> response = authService.getAllUsers();
        return ResponseEntity.ok(new ApiResponse<>(true, "Users retrieved successfully", response));
    }

    @PostMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserSummaryDTO>> adminCreateUser(
            @Valid @RequestBody AdminCreateUserRequest request) {
        UserSummaryDTO response = authService.adminCreateUser(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Account created successfully", response));
    }

    @DeleteMapping("/admin/users/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> adminDeleteUser(
            @PathVariable String username, Authentication authentication) {
        authService.adminDeleteUser(username, authentication.getName());
        return ResponseEntity.ok(new ApiResponse<>(true, "Account deleted successfully", null));
    }

    @PutMapping("/admin/users/{username}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserSummaryDTO>> updateUserRole(
            @PathVariable String username,
            @Valid @RequestBody ChangeUserRoleRequest request) {
        UserSummaryDTO response = authService.updateUserRole(username, request.getRole());
        return ResponseEntity.ok(new ApiResponse<>(true, "Account role updated successfully", response));
    }

    /**
     * Admin-only: forcibly reset a user's password (for when they've forgotten it and
     * can't provide the old one). Restricted to ADMIN in SecurityConfig.
     */
    @PostMapping("/admin/users/{username}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> adminResetPassword(
            @PathVariable String username, @Valid @RequestBody AdminPasswordResetRequest request) {
        authService.adminResetPassword(username, request.getNewPassword());
        return ResponseEntity.ok(new ApiResponse<>(true, "Password reset for user '" + username + "' successfully", null));
    }
}
