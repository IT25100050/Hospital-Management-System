package com.hospital.hms.doctorrecords.dto;

import com.hospital.hms.doctorrecords.model.DoctorApplication;
import com.hospital.hms.doctorrecords.model.DoctorApplicationStatus;

import java.time.LocalDateTime;

public class DoctorApplicationResponse {
    private final Long id;
    private final String name;
    private final String email;
    private final String phoneNumber;
    private final String medicalRegistrationNumber;
    private final String specialty;
    private final String qualifications;
    private final Integer experience;
    private final DoctorApplicationStatus status;
    private final String rejectionReason;
    private final String reviewer;
    private final LocalDateTime reviewedAt;

    public DoctorApplicationResponse(DoctorApplication application) {
        id = application.getId();
        name = application.getName();
        email = application.getEmail();
        phoneNumber = application.getPhoneNumber();
        medicalRegistrationNumber = application.getMedicalRegistrationNumber();
        specialty = application.getSpecialty();
        qualifications = application.getQualifications();
        experience = application.getExperience();
        status = application.getStatus();
        rejectionReason = application.getRejectionReason();
        reviewer = application.getReviewedBy() == null ? null : application.getReviewedBy().getUsername();
        reviewedAt = application.getReviewedAt();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getMedicalRegistrationNumber() { return medicalRegistrationNumber; }
    public String getSpecialty() { return specialty; }
    public String getQualifications() { return qualifications; }
    public Integer getExperience() { return experience; }
    public DoctorApplicationStatus getStatus() { return status; }
    public String getRejectionReason() { return rejectionReason; }
    public String getReviewer() { return reviewer; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
}
