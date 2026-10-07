package com.hospital.hms.appointment.controller;

import com.hospital.hms.common.enums.AppointmentStatus;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

public class AppointmentDecisionRequest {
    @NotNull
    private AppointmentStatus status;
    @Size(max = 1000)
    private String rejectionReason;

    public AppointmentStatus getStatus() { return status; }
    public void setStatus(AppointmentStatus status) { this.status = status; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}
