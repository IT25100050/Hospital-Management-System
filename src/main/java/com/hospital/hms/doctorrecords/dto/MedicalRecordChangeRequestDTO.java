package com.hospital.hms.doctorrecords.dto;

import com.hospital.hms.doctorrecords.model.MedicalRecordChangeType;
import jakarta.validation.constraints.NotNull;

public class MedicalRecordChangeRequestDTO {
    @NotNull(message = "Change type is required")
    private MedicalRecordChangeType changeType;
    private String diagnosis;
    private String treatment;
    private String notes;
    private String requestReason;

    public MedicalRecordChangeType getChangeType() { return changeType; }
    public void setChangeType(MedicalRecordChangeType changeType) { this.changeType = changeType; }
    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
    public String getTreatment() { return treatment; }
    public void setTreatment(String treatment) { this.treatment = treatment; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getRequestReason() { return requestReason; }
    public void setRequestReason(String requestReason) { this.requestReason = requestReason; }
}
