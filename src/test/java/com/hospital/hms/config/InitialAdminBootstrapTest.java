package com.hospital.hms.config;

import com.hospital.hms.auth.model.User;
import com.hospital.hms.auth.repository.UserRepository;
import com.hospital.hms.common.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InitialAdminBootstrapTest {
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @Test
    void bootstrapCreatesHashedAdminOnlyWhenOneDoesNotExist() {
        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(false);
        when(passwordEncoder.encode("A-long-bootstrap-password")).thenReturn("bcrypt-hash");

        new InitialAdminBootstrap(userRepository, passwordEncoder, "hospital-admin",
                "admin@example.test", "A-long-bootstrap-password")
                .run(new DefaultApplicationArguments(new String[0]));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(UserRole.ADMIN, captor.getValue().getRole());
        assertEquals("bcrypt-hash", captor.getValue().getPassword());
    }

    @Test
    void bootstrapDoesNotReplaceExistingAdmin() {
        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(true);

        new InitialAdminBootstrap(userRepository, passwordEncoder, "replacement",
                "replacement@example.test", "A-long-bootstrap-password")
                .run(new DefaultApplicationArguments(new String[0]));

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder);
    }
}
