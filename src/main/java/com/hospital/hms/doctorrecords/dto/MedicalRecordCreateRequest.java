package com.hospital.hms.doctorrecords.dto;

import jakarta.validation.constraints.NotBlank;

public class MedicalRecordCreateRequest {
    @NotBlank(message = "Diagnosis is required")
    private String diagnosis;
    private String treatment;
    private String notes;

    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
    public String getTreatment() { return treatment; }
    public void setTreatment(String treatment) { this.treatment = treatment; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
