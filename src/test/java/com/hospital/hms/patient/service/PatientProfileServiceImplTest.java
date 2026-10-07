package com.hospital.hms.patient.service;

import com.hospital.hms.auth.model.User;
import com.hospital.hms.auth.repository.UserRepository;
import com.hospital.hms.common.enums.UserRole;
import com.hospital.hms.patient.model.Patient;
import com.hospital.hms.patient.repository.PatientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientProfileServiceImplTest {
    @Mock private UserRepository userRepository;
    @Mock private PatientRepository patientRepository;
    @InjectMocks private PatientProfileServiceImpl service;

    @Test
    void createsAndLinksProfileForExistingPatientAccountWithoutProfile() {
        User user = new User("alex", "alex@example.test", "hash", UserRole.PATIENT);
        when(userRepository.findByUsername("alex")).thenReturn(Optional.of(user));
        when(patientRepository.findByUserUsername("alex")).thenReturn(Optional.empty());
        when(patientRepository.findAllByEmailIgnoreCase("alex@example.test")).thenReturn(List.of());
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Patient result = service.getOrCreateForUser("alex");

        ArgumentCaptor<Patient> patientCaptor = ArgumentCaptor.forClass(Patient.class);
        verify(patientRepository).save(patientCaptor.capture());
        assertEquals("alex", result.getName());
        assertEquals("alex@example.test", result.getEmail());
        assertEquals(user, patientCaptor.getValue().getUser());
    }

    @Test
    void linksExistingUnclaimedProfileForPatientAccount() {
        User user = new User("alex", "alex@example.test", "hash", UserRole.PATIENT);
        Patient existingProfile = new Patient("Alex Patient", "alex@example.test", "0771234567");
        when(userRepository.findByUsername("alex")).thenReturn(Optional.of(user));
        when(patientRepository.findByUserUsername("alex")).thenReturn(Optional.empty());
        when(patientRepository.findAllByEmailIgnoreCase("alex@example.test")).thenReturn(List.of(existingProfile));
        when(patientRepository.save(existingProfile)).thenReturn(existingProfile);

        Patient result = service.getOrCreateForUser("alex");

        assertEquals(existingProfile, result);
        assertEquals(user, result.getUser());
        assertEquals("Alex Patient", result.getName());
        assertEquals("0771234567", result.getPhoneNumber());
    }
}
