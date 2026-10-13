package com.hospital.hms.doctorrecords.dto;

import com.hospital.hms.doctorrecords.model.DoctorApplicationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class DoctorApplicationReviewRequest {
    @NotNull
    private DoctorApplicationStatus status;
    @Size(max = 1000)
    private String rejectionReason;

    public DoctorApplicationStatus getStatus() { return status; }
    public void setStatus(DoctorApplicationStatus status) { this.status = status; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}
