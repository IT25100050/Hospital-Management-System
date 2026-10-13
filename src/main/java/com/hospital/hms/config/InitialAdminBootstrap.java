package com.hospital.hms.config;

import com.hospital.hms.auth.model.User;
import com.hospital.hms.auth.repository.UserRepository;
import com.hospital.hms.common.enums.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class InitialAdminBootstrap implements ApplicationRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String email;
    private final String password;

    public InitialAdminBootstrap(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                 @Value("${hms.bootstrap-admin.username:}") String username,
                                 @Value("${hms.bootstrap-admin.email:}") String email,
                                 @Value("${hms.bootstrap-admin.password:}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(UserRole.ADMIN)) return;
        boolean configured = !username.isBlank() || !email.isBlank() || !password.isBlank();
        if (!configured) return;
        if (username.isBlank() || email.isBlank() || password.length() < 12) {
            throw new IllegalStateException("Configure the bootstrap admin username, email, and a password of at least 12 characters.");
        }
        if (userRepository.existsByUsername(username) || userRepository.existsByEmail(email)) {
            throw new IllegalStateException("Bootstrap admin username or email already belongs to another account.");
        }
        userRepository.save(new User(username, email, passwordEncoder.encode(password), UserRole.ADMIN));
    }
}
