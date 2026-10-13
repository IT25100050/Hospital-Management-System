package com.hospital.hms.doctorrecords.service;

import com.hospital.hms.auth.model.User;
import com.hospital.hms.auth.repository.UserRepository;
import com.hospital.hms.common.enums.UserRole;
import com.hospital.hms.common.exception.BadRequestException;
import com.hospital.hms.doctorrecords.dto.DoctorApplicationRequest;
import com.hospital.hms.doctorrecords.dto.DoctorApplicationReviewRequest;
import com.hospital.hms.doctorrecords.model.Doctor;
import com.hospital.hms.doctorrecords.model.DoctorApplication;
import com.hospital.hms.doctorrecords.model.DoctorApplicationStatus;
import com.hospital.hms.doctorrecords.repository.DoctorApplicationRepository;
import com.hospital.hms.doctorrecords.repository.DoctorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DoctorApplicationServiceTest {
    @Mock private DoctorApplicationRepository applicationRepository;
    @Mock private DoctorRepository doctorRepository;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private DoctorApplicationService service;

    @Test
    void submissionStoresOnlyAnEncodedPasswordAndDoesNotCreateDoctorAccount() {
        DoctorApplicationRequest request = request();
        when(passwordEncoder.encode("secure-pass")).thenReturn("bcrypt-value");
        when(applicationRepository.save(any(DoctorApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.submit(request);

        ArgumentCaptor<DoctorApplication> captor = ArgumentCaptor.forClass(DoctorApplication.class);
        verify(applicationRepository).save(captor.capture());
        assertEquals("bcrypt-value", captor.getValue().getPasswordHash());
        assertEquals(DoctorApplicationStatus.PENDING, response.getStatus());
        assertFalse(java.util.Arrays.stream(response.getClass().getMethods())
                .anyMatch(method -> method.getName().toLowerCase().contains("password")));
        verify(doctorRepository, never()).save(any(Doctor.class));
        verify(userRepository, never()).save(any());
    }

    @Test
    void approvalCreatesDoctorAccountAndProfileAndCannotBeRepeated() {
        DoctorApplication application = new DoctorApplication("Dr Test", "doctor@example.com",
                "bcrypt-value", "5555555555", "REG-1", "Cardiology", "MBBS", 7);
        User reviewer = new User("manager", "manager@example.com", "hash", UserRole.ADMIN);
        when(applicationRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(DoctorApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(reviewer));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DoctorApplicationReviewRequest request = new DoctorApplicationReviewRequest();
        request.setStatus(DoctorApplicationStatus.APPROVED);

        var response = service.review(4L, request, "manager");

        assertEquals(DoctorApplicationStatus.APPROVED, response.getStatus());
        assertNotNull(response.getReviewedAt());
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(UserRole.DOCTOR, userCaptor.getValue().getRole());
        ArgumentCaptor<Doctor> doctorCaptor = ArgumentCaptor.forClass(Doctor.class);
        verify(doctorRepository).save(doctorCaptor.capture());
        assertTrue(doctorCaptor.getValue().getActive());
        assertEquals("bcrypt-value", userCaptor.getValue().getPassword());
        assertThrows(BadRequestException.class, () -> service.review(4L, request, "manager"));
        verify(userRepository, times(1)).save(any(User.class));
        verify(doctorRepository, times(1)).save(any(Doctor.class));
    }

    private DoctorApplicationRequest request() {
        DoctorApplicationRequest request = new DoctorApplicationRequest();
        request.setName("Dr Test");
        request.setEmail("doctor@example.com");
        request.setPassword("secure-pass");
        request.setPhoneNumber("5555555555");
        request.setMedicalRegistrationNumber("REG-1");
        request.setSpecialty("Cardiology");
        request.setQualifications("MBBS");
        request.setExperience(7);
        return request;
    }
}
