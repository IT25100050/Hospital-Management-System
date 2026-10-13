package com.hospital.hms.doctorrecords.model;

import com.hospital.hms.auth.model.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "medical_record_change_requests")
public class MedicalRecordChangeRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long medicalRecordId;

    @Column(nullable = false)
    private Long doctorId;

    @ManyToOne
    @JoinColumn(name = "requested_by_user_id")
    private User requestedBy;

    @Column(length = 100)
    private String requestedByUsername;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private MedicalRecordChangeType changeType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private MedicalRecordChangeStatus status = MedicalRecordChangeStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String diagnosis;

    @Column(columnDefinition = "TEXT")
    private String treatment;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(columnDefinition = "TEXT")
    private String originalDiagnosis;

    @Column(columnDefinition = "TEXT")
    private String originalTreatment;

    @Column(columnDefinition = "TEXT")
    private String originalNotes;

    @Column(length = 1000)
    private String requestReason;

    @ManyToOne
    @JoinColumn(name = "reviewed_by_user_id")
    private User reviewedBy;

    private LocalDateTime requestedAt;
    private LocalDateTime reviewedAt;

    @Column(length = 1000)
    private String reviewReason;

    protected MedicalRecordChangeRequest() {}

    public MedicalRecordChangeRequest(Long medicalRecordId, Long doctorId, User requestedBy,
                                      MedicalRecordChangeType changeType, String diagnosis,
                                      String treatment, String notes, String originalDiagnosis,
                                      String originalTreatment, String originalNotes, String requestReason,
                                      LocalDateTime requestedAt) {
        this.medicalRecordId = medicalRecordId;
        this.doctorId = doctorId;
        this.requestedBy = requestedBy;
        this.requestedByUsername = requestedBy.getUsername();
        this.changeType = changeType;
        this.diagnosis = diagnosis;
        this.treatment = treatment;
        this.notes = notes;
        this.originalDiagnosis = originalDiagnosis;
        this.originalTreatment = originalTreatment;
        this.originalNotes = originalNotes;
        this.requestReason = requestReason;
        this.requestedAt = requestedAt;
    }

    public Long getId() { return id; }
    public Long getMedicalRecordId() { return medicalRecordId; }
    public Long getDoctorId() { return doctorId; }
    public User getRequestedBy() { return requestedBy; }
    public void setRequestedBy(User requestedBy) { this.requestedBy = requestedBy; }
    public String getRequestedByUsername() { return requestedByUsername; }
    public void setRequestedByUsername(String requestedByUsername) { this.requestedByUsername = requestedByUsername; }
    public MedicalRecordChangeType getChangeType() { return changeType; }
    public MedicalRecordChangeStatus getStatus() { return status; }
    public void setStatus(MedicalRecordChangeStatus status) { this.status = status; }
    public String getDiagnosis() { return diagnosis; }
    public String getTreatment() { return treatment; }
    public String getNotes() { return notes; }
    public String getOriginalDiagnosis() { return originalDiagnosis; }
    public String getOriginalTreatment() { return originalTreatment; }
    public String getOriginalNotes() { return originalNotes; }
    public String getRequestReason() { return requestReason; }
    public User getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(User reviewedBy) { this.reviewedBy = reviewedBy; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public String getReviewReason() { return reviewReason; }
    public void setReviewReason(String reviewReason) { this.reviewReason = reviewReason; }
}
