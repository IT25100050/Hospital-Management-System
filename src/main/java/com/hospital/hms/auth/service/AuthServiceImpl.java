package com.hospital.hms.auth.service;

import com.hospital.hms.auth.dto.*;
import com.hospital.hms.auth.model.User;
import com.hospital.hms.auth.repository.UserRepository;
import com.hospital.hms.auth.security.JwtTokenProvider;
import com.hospital.hms.common.enums.UserRole;
import com.hospital.hms.common.exception.BadRequestException;
import com.hospital.hms.common.exception.DuplicateResourceException;
import com.hospital.hms.common.exception.ResourceNotFoundException;
import com.hospital.hms.doctorrecords.repository.DoctorRepository;
import com.hospital.hms.doctorrecords.model.Doctor;
import com.hospital.hms.doctorrecords.repository.DoctorApplicationRepository;
import com.hospital.hms.doctorrecords.repository.MedicalRecordChangeRequestRepository;
import com.hospital.hms.patient.repository.PatientRepository;
import com.hospital.hms.patient.service.PatientProfileService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final PatientProfileService patientProfileService;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final DoctorApplicationRepository doctorApplicationRepository;
    private final MedicalRecordChangeRequestRepository changeRequestRepository;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider, PatientProfileService patientProfileService,
                           DoctorRepository doctorRepository, PatientRepository patientRepository,
                           DoctorApplicationRepository doctorApplicationRepository,
                           MedicalRecordChangeRequestRepository changeRequestRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.patientProfileService = patientProfileService;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.doctorApplicationRepository = doctorApplicationRepository;
        this.changeRequestRepository = changeRequestRepository;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameOrEmail(request.getUsername(), request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())
                || (user.getRole() == UserRole.DOCTOR
                && doctorRepository.findByUserUsername(user.getUsername())
                .filter(doctor -> Boolean.TRUE.equals(doctor.getActive())).isEmpty())) {
            throw new BadRequestException("Invalid credentials");
        }

        if (user.getRole() == UserRole.PATIENT) {
            patientProfileService.getOrCreateForUser(user.getUsername());
        }

        Authentication authentication = new UsernamePasswordAuthenticationToken(user.getUsername(), null);
        String token = jwtTokenProvider.generateToken(authentication);

        return new LoginResponse(token, user.getUsername(), user.getRole());
    }

    @Override
    @Transactional
    public String register(RegisterRequest request) {
        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("Username is already taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email is already in use");
        }

        User user = new User(username, email,
                passwordEncoder.encode(request.getPassword()), UserRole.PATIENT);

        User savedUser = userRepository.save(user);
        patientProfileService.getOrCreateForUser(savedUser.getUsername());
        return "User registered successfully";
    }

    @Override
    public void resetPassword(PasswordResetRequest request, String authenticatedUsername) {
        User user = userRepository.findByUsername(authenticatedUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadRequestException("Incorrect old password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    public List<UserSummaryDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(u -> new UserSummaryDTO(u.getId(), u.getUsername(), u.getEmail(), u.getRole()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserSummaryDTO adminCreateUser(AdminCreateUserRequest request) {
        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("Username is already taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email is already in use");
        }
        if (request.getRole() == UserRole.DOCTOR) {
            throw new BadRequestException("Doctor accounts can only be created by approving a doctor registration.");
        }

        User user = userRepository.save(new User(username, email,
                passwordEncoder.encode(request.getPassword()), request.getRole()));
        if (request.getRole() == UserRole.PATIENT) {
            patientProfileService.getOrCreateForUser(user.getUsername());
        }
        return new UserSummaryDTO(user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }

    @Override
    @Transactional
    public void adminDeleteUser(String username, String authenticatedAdminUsername) {
        if (username.equals(authenticatedAdminUsername)) {
            throw new BadRequestException("You cannot delete the account you are currently using.");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        if (user.getRole() == UserRole.ADMIN && userRepository.countByRole(UserRole.ADMIN) <= 1) {
            throw new BadRequestException("The hospital must retain at least one administrator.");
        }

        doctorRepository.findByUserUsername(username).ifPresent(doctor -> {
            doctor.setActive(false);
            doctor.setUser(null);
            doctorRepository.save(doctor);
        });
        patientRepository.findByUserUsername(username).ifPresent(patient -> {
            patient.setUser(null);
            patient.setEmail(null);
            patientRepository.save(patient);
        });
        doctorApplicationRepository.detachReviewer(user.getId());
        changeRequestRepository.detachRequester(user.getId(), user.getUsername());
        changeRequestRepository.detachReviewer(user.getId());
        userRepository.delete(user);
    }

    @Override
    @Transactional
    public UserSummaryDTO updateUserRole(String username, UserRole role) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        if (user.getRole() == role) {
            return new UserSummaryDTO(user.getId(), user.getUsername(), user.getEmail(), user.getRole());
        }
        if (user.getRole() == UserRole.ADMIN
                && role != UserRole.ADMIN
                && userRepository.countByRole(UserRole.ADMIN) <= 1) {
            throw new BadRequestException("The hospital must retain at least one administrator. Create another ADMIN before changing this role.");
        }

        Doctor doctor = doctorRepository.findByUserUsername(username).orElse(null);
        if (role == UserRole.DOCTOR && doctor == null) {
            throw new BadRequestException(
                    "Only an account with an approved doctor profile can be assigned the DOCTOR role.");
        }
        user.setRole(role);
        if (role == UserRole.PATIENT) {
            patientProfileService.getOrCreateForUser(username);
        }

        if (doctor != null) {
            doctor.setActive(role == UserRole.DOCTOR);
            doctorRepository.save(doctor);
        }
        User updated = userRepository.save(user);
        return new UserSummaryDTO(updated.getId(), updated.getUsername(), updated.getEmail(), updated.getRole());
    }

    @Override
    public void adminResetPassword(String username, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
