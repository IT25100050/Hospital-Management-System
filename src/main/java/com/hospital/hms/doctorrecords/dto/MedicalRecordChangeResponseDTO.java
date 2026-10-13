package com.hospital.hms.doctorrecords.dto;

import com.hospital.hms.doctorrecords.model.MedicalRecordChangeRequest;
import com.hospital.hms.doctorrecords.model.MedicalRecordChangeStatus;
import com.hospital.hms.doctorrecords.model.MedicalRecordChangeType;

import java.time.LocalDateTime;

public class MedicalRecordChangeResponseDTO {
    private final Long id;
    private final Long medicalRecordId;
    private final Long doctorId;
    private final String patientName;
    private final String requestedBy;
    private final MedicalRecordChangeType changeType;
    private final MedicalRecordChangeStatus status;
    private final String diagnosis;
    private final String treatment;
    private final String notes;
    private final String currentDiagnosis;
    private final String currentTreatment;
    private final String currentNotes;
    private final String requestReason;
    private final LocalDateTime requestedAt;
    private final LocalDateTime reviewedAt;
    private final String reviewReason;

    public MedicalRecordChangeResponseDTO(MedicalRecordChangeRequest request, String patientName) {
        this.id = request.getId();
        this.medicalRecordId = request.getMedicalRecordId();
        this.doctorId = request.getDoctorId();
        this.patientName = patientName;
        this.requestedBy = request.getRequestedByUsername() != null
                ? request.getRequestedByUsername()
                : request.getRequestedBy().getUsername();
        this.changeType = request.getChangeType();
        this.status = request.getStatus();
        this.diagnosis = request.getDiagnosis();
        this.treatment = request.getTreatment();
        this.notes = request.getNotes();
        this.currentDiagnosis = request.getOriginalDiagnosis();
        this.currentTreatment = request.getOriginalTreatment();
        this.currentNotes = request.getOriginalNotes();
        this.requestReason = request.getRequestReason();
        this.requestedAt = request.getRequestedAt();
        this.reviewedAt = request.getReviewedAt();
        this.reviewReason = request.getReviewReason();
    }

    public Long getId() { return id; }
    public Long getMedicalRecordId() { return medicalRecordId; }
    public Long getDoctorId() { return doctorId; }
    public String getPatientName() { return patientName; }
    public String getRequestedBy() { return requestedBy; }
    public MedicalRecordChangeType getChangeType() { return changeType; }
    public MedicalRecordChangeStatus getStatus() { return status; }
    public String getDiagnosis() { return diagnosis; }
    public String getTreatment() { return treatment; }
    public String getNotes() { return notes; }
    public String getCurrentDiagnosis() { return currentDiagnosis; }
    public String getCurrentTreatment() { return currentTreatment; }
    public String getCurrentNotes() { return currentNotes; }
    public String getRequestReason() { return requestReason; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public String getReviewReason() { return reviewReason; }
}
