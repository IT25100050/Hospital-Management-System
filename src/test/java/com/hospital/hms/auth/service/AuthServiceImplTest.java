package com.hospital.hms.auth.service;

import com.hospital.hms.auth.dto.RegisterRequest;
import com.hospital.hms.auth.model.User;
import com.hospital.hms.auth.repository.UserRepository;
import com.hospital.hms.auth.security.JwtTokenProvider;
import com.hospital.hms.common.enums.UserRole;
import com.hospital.hms.common.exception.BadRequestException;
import com.hospital.hms.doctorrecords.model.Doctor;
import com.hospital.hms.doctorrecords.repository.DoctorApplicationRepository;
import com.hospital.hms.doctorrecords.repository.DoctorRepository;
import com.hospital.hms.doctorrecords.repository.MedicalRecordChangeRequestRepository;
import com.hospital.hms.patient.repository.PatientRepository;
import com.hospital.hms.patient.service.PatientProfileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private PatientProfileService patientProfileService;
    @Mock private DoctorRepository doctorRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private DoctorApplicationRepository doctorApplicationRepository;
    @Mock private MedicalRecordChangeRequestRepository changeRequestRepository;
    @InjectMocks private AuthServiceImpl service;

    @Test
    void registrationAlwaysCreatesPatientAndLinksPatientProfile() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("  Alex Patient  ");
        request.setEmail(" ALEX@example.com ");
        request.setPassword("password123");
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(UserRole.PATIENT, userCaptor.getValue().getRole());
        assertEquals("Alex Patient", userCaptor.getValue().getUsername());
        assertEquals("alex@example.com", userCaptor.getValue().getEmail());
        assertEquals("encoded-password", userCaptor.getValue().getPassword());
        verify(patientProfileService).getOrCreateForUser("Alex Patient");
    }

    @Test
    void adminCanRestoreDoctorRoleOnlyForAnExistingApprovedDoctorProfile() {
        User user = new User("doctor-login", "doctor@example.test", "hash", UserRole.PATIENT);
        Doctor doctor = new Doctor("Dr Example", "Cardiology", "doctor@example.test", null, null);
        doctor.setActive(false);
        when(userRepository.findByUsername("doctor-login")).thenReturn(java.util.Optional.of(user));
        when(doctorRepository.findByUserUsername("doctor-login")).thenReturn(java.util.Optional.of(doctor));
        when(userRepository.save(user)).thenReturn(user);

        var result = service.updateUserRole("doctor-login", UserRole.DOCTOR);

        assertEquals(UserRole.DOCTOR, result.getRole());
        assertEquals(UserRole.DOCTOR, user.getRole());
        assertEquals(true, doctor.getActive());
        verify(doctorRepository).save(doctor);
    }

    @Test
    void adminCannotGrantDoctorRoleWithoutApprovedDoctorProfile() {
        User user = new User("regular-user", "user@example.test", "hash", UserRole.PATIENT);
        when(userRepository.findByUsername("regular-user")).thenReturn(java.util.Optional.of(user));
        when(doctorRepository.findByUserUsername("regular-user")).thenReturn(java.util.Optional.empty());

        assertThrows(BadRequestException.class,
                () -> service.updateUserRole("regular-user", UserRole.DOCTOR));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void adminCannotRemoveTheLastAdministrator() {
        User admin = new User("admin", "admin@example.test", "hash", UserRole.ADMIN);
        when(userRepository.findByUsername("admin")).thenReturn(java.util.Optional.of(admin));
        when(userRepository.countByRole(UserRole.ADMIN)).thenReturn(1L);

        assertThrows(BadRequestException.class,
                () -> service.updateUserRole("admin", UserRole.PATIENT));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void administratorCanSwitchToRestrictedRoleWhenAnotherAdministratorRemains() {
        User admin = new User("records-admin", "records-admin@example.test", "hash", UserRole.ADMIN);
        when(userRepository.findByUsername("records-admin")).thenReturn(java.util.Optional.of(admin));
        when(userRepository.countByRole(UserRole.ADMIN)).thenReturn(2L);
        when(doctorRepository.findByUserUsername("records-admin")).thenReturn(java.util.Optional.empty());
        when(userRepository.save(admin)).thenReturn(admin);

        var result = service.updateUserRole("records-admin", UserRole.DOCTOR_RECORDS_MANAGER);

        assertEquals(UserRole.DOCTOR_RECORDS_MANAGER, result.getRole());
        assertEquals(UserRole.DOCTOR_RECORDS_MANAGER, admin.getRole());
    }

    @Test
    void adminCannotDeleteOwnAccount() {
        assertThrows(BadRequestException.class,
                () -> service.adminDeleteUser("admin", "admin"));
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void adminCannotDeleteTheLastAdministrator() {
        User admin = new User("only-admin", "admin@example.test", "hash", UserRole.ADMIN);
        when(userRepository.findByUsername("only-admin")).thenReturn(java.util.Optional.of(admin));
        when(userRepository.countByRole(UserRole.ADMIN)).thenReturn(1L);

        assertThrows(BadRequestException.class,
                () -> service.adminDeleteUser("only-admin", "other-admin"));
        verify(userRepository, never()).delete(any(User.class));
    }
}
