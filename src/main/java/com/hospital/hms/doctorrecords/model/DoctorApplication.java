package com.hospital.hms.doctorrecords.model;

import com.hospital.hms.auth.model.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "doctor_applications", uniqueConstraints = {
        @UniqueConstraint(columnNames = "email"),
        @UniqueConstraint(columnNames = "medical_registration_number")
})
public class DoctorApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;
    @Column(nullable = false, unique = true)
    private String email;
    @Column(nullable = false)
    private String passwordHash;
    @Column(nullable = false)
    private String phoneNumber;
    @Column(nullable = false, unique = true)
    private String medicalRegistrationNumber;
    @Column(nullable = false)
    private String specialty;
    @Column(nullable = false)
    private String qualifications;
    @Column(nullable = false)
    private Integer experience;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DoctorApplicationStatus status = DoctorApplicationStatus.PENDING;

    @ManyToOne
    @JoinColumn(name = "reviewed_by_user_id")
    private User reviewedBy;
    private LocalDateTime reviewedAt;
    @Column(length = 1000)
    private String rejectionReason;

    protected DoctorApplication() {}

    public DoctorApplication(String name, String email, String passwordHash, String phoneNumber,
                             String medicalRegistrationNumber, String specialty, String qualifications,
                             Integer experience) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.phoneNumber = phoneNumber;
        this.medicalRegistrationNumber = medicalRegistrationNumber;
        this.specialty = specialty;
        this.qualifications = qualifications;
        this.experience = experience;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getMedicalRegistrationNumber() { return medicalRegistrationNumber; }
    public String getSpecialty() { return specialty; }
    public String getQualifications() { return qualifications; }
    public Integer getExperience() { return experience; }
    public DoctorApplicationStatus getStatus() { return status; }
    public void setStatus(DoctorApplicationStatus status) { this.status = status; }
    public User getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(User reviewedBy) { this.reviewedBy = reviewedBy; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}
