package com.hospital.hms.patient.service;

import com.hospital.hms.auth.model.User;
import com.hospital.hms.auth.repository.UserRepository;
import com.hospital.hms.common.enums.UserRole;
import com.hospital.hms.common.exception.ResourceNotFoundException;
import com.hospital.hms.patient.model.Patient;
import com.hospital.hms.patient.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PatientProfileServiceImpl implements PatientProfileService {
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;

    public PatientProfileServiceImpl(UserRepository userRepository, PatientRepository patientRepository) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
    }

    @Override
    @Transactional
    public Patient getOrCreateForUser(String username) {
        User user = userRepository.findByUsername(username)
                .filter(account -> account.getRole() == UserRole.PATIENT)
                .orElseThrow(() -> new ResourceNotFoundException("Patient account not found."));

        return patientRepository.findByUserUsername(username).orElseGet(() -> {
            List<Patient> profilesByEmail = patientRepository.findAllByEmailIgnoreCase(user.getEmail());
            Patient patient = profilesByEmail.size() == 1 && profilesByEmail.get(0).getUser() == null
                    ? profilesByEmail.get(0)
                    : new Patient(user.getUsername(), user.getEmail(), null);
            patient.setUser(user);
            return patientRepository.save(patient);
        });
    }
}
