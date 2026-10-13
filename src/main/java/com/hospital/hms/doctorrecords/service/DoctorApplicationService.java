package com.hospital.hms.doctorrecords.service;

import com.hospital.hms.auth.model.User;
import com.hospital.hms.auth.repository.UserRepository;
import com.hospital.hms.common.enums.UserRole;
import com.hospital.hms.common.exception.BadRequestException;
import com.hospital.hms.common.exception.DuplicateResourceException;
import com.hospital.hms.common.exception.ResourceNotFoundException;
import com.hospital.hms.doctorrecords.dto.*;
import com.hospital.hms.doctorrecords.model.*;
import com.hospital.hms.doctorrecords.repository.DoctorApplicationRepository;
import com.hospital.hms.doctorrecords.repository.DoctorRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DoctorApplicationService {
    private final DoctorApplicationRepository applicationRepository;
    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DoctorApplicationService(DoctorApplicationRepository applicationRepository,
                                    DoctorRepository doctorRepository, UserRepository userRepository,
                                    PasswordEncoder passwordEncoder) {
        this.applicationRepository = applicationRepository;
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public DoctorApplicationResponse submit(DoctorApplicationRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String registrationNumber = request.getMedicalRegistrationNumber().trim();
        if (userRepository.existsByEmail(email) || userRepository.existsByUsername(email)
                || applicationRepository.existsByEmail(email) || doctorRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("An account or application already uses this email address.");
        }
        if (applicationRepository.existsByMedicalRegistrationNumber(registrationNumber)
                || doctorRepository.existsByMedicalRegistrationNumber(registrationNumber)) {
            throw new DuplicateResourceException("This medical registration number is already in use.");
        }
        DoctorApplication application = new DoctorApplication(
                request.getName().trim(), email, passwordEncoder.encode(request.getPassword()),
                request.getPhoneNumber().trim(), registrationNumber, request.getSpecialty().trim(),
                request.getQualifications().trim(), request.getExperience());
        return new DoctorApplicationResponse(applicationRepository.save(application));
    }

    @Transactional(readOnly = true)
    public List<DoctorApplicationResponse> list() {
        return applicationRepository.findAllByOrderByIdDesc().stream().map(DoctorApplicationResponse::new).toList();
    }

    @Transactional
    public DoctorApplicationResponse review(Long id, DoctorApplicationReviewRequest request, String reviewerUsername) {
        DoctorApplication application = applicationRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor registration request not found."));
        if (application.getStatus() != DoctorApplicationStatus.PENDING) {
            throw new BadRequestException("This doctor registration request has already been reviewed.");
        }
        if (request.getStatus() != DoctorApplicationStatus.APPROVED
                && request.getStatus() != DoctorApplicationStatus.REJECTED) {
            throw new BadRequestException("A request can only be approved or rejected.");
        }

        User reviewer = userRepository.findByUsername(reviewerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Reviewer account not found."));
        if (request.getStatus() == DoctorApplicationStatus.REJECTED) {
            if (request.getRejectionReason() == null || request.getRejectionReason().isBlank()) {
                throw new BadRequestException("A reason is required when rejecting an application.");
            }
            application.setRejectionReason(request.getRejectionReason().trim());
        } else {
            if (userRepository.existsByEmail(application.getEmail())
                    || userRepository.existsByUsername(application.getEmail())
                    || doctorRepository.existsByEmail(application.getEmail())
                    || doctorRepository.existsByMedicalRegistrationNumber(application.getMedicalRegistrationNumber())) {
                throw new DuplicateResourceException("An account or doctor profile already uses this application information.");
            }
            User doctorUser = userRepository.save(new User(
                    application.getEmail(), application.getEmail(), application.getPasswordHash(), UserRole.DOCTOR));
            Doctor doctor = new Doctor(application.getName(), application.getSpecialty(), application.getEmail(),
                    application.getPhoneNumber(), null);
            doctor.setUser(doctorUser);
            doctor.setMedicalRegistrationNumber(application.getMedicalRegistrationNumber());
            doctor.setQualifications(application.getQualifications());
            doctor.setExperience(application.getExperience());
            doctor.setActive(true);
            doctorRepository.save(doctor);
            application.setRejectionReason(null);
        }
        application.setStatus(request.getStatus());
        application.setReviewedBy(reviewer);
        application.setReviewedAt(LocalDateTime.now());
        return new DoctorApplicationResponse(applicationRepository.save(application));
    }
}
