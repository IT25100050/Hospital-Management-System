package com.hospital.hms.auth.repository;

import com.hospital.hms.auth.model.User;
import com.hospital.hms.common.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByUsernameOrEmail(String username, String email);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
    boolean existsByRole(UserRole role);
    long countByRole(UserRole role);
}