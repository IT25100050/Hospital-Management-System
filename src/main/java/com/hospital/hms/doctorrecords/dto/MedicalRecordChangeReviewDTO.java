package com.hospital.hms.doctorrecords.dto;

import com.hospital.hms.doctorrecords.model.MedicalRecordChangeStatus;
import jakarta.validation.constraints.NotNull;

public class MedicalRecordChangeReviewDTO {
    @NotNull(message = "Review decision is required")
    private MedicalRecordChangeStatus status;
    private String reviewReason;

    public MedicalRecordChangeStatus getStatus() { return status; }
    public void setStatus(MedicalRecordChangeStatus status) { this.status = status; }
    public String getReviewReason() { return reviewReason; }
    public void setReviewReason(String reviewReason) { this.reviewReason = reviewReason; }
}
