package com.hospital.hms.appointment.dto;

import com.hospital.hms.common.enums.AppointmentStatus;

import java.time.LocalDateTime;

public class AppointmentResponseDTO {
    private Long id;
    private Long patientId;
    private String patientName;
    private Long doctorId;
    private LocalDateTime appointmentTime;
    private AppointmentStatus status;
    private String reason;
    private String doctorName;
    private String doctorSpecialty;
    private String patientEmail;
    private String patientPhoneNumber;
    private LocalDateTime decisionTime;
    private String rejectionReason;

    public AppointmentResponseDTO() {}

    public AppointmentResponseDTO(Long id, Long patientId, String patientName, Long doctorId, LocalDateTime appointmentTime, AppointmentStatus status) {
        this.id = id;
        this.patientId = patientId;
        this.patientName = patientName;
        this.doctorId = doctorId;
        this.appointmentTime = appointmentTime;
        this.status = status;
    }

    public AppointmentResponseDTO(Long id, Long patientId, String patientName, Long doctorId,
                                  LocalDateTime appointmentTime, AppointmentStatus status, String reason,
                                  String doctorName, String doctorSpecialty, String patientEmail,
                                  String patientPhoneNumber, LocalDateTime decisionTime, String rejectionReason) {
        this(id, patientId, patientName, doctorId, appointmentTime, status);
        this.reason = reason;
        this.doctorName = doctorName;
        this.doctorSpecialty = doctorSpecialty;
        this.patientEmail = patientEmail;
        this.patientPhoneNumber = patientPhoneNumber;
        this.decisionTime = decisionTime;
        this.rejectionReason = rejectionReason;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }
    public LocalDateTime getAppointmentTime() { return appointmentTime; }
    public void setAppointmentTime(LocalDateTime appointmentTime) { this.appointmentTime = appointmentTime; }
    public AppointmentStatus getStatus() { return status; }
    public void setStatus(AppointmentStatus status) { this.status = status; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }
    public String getDoctorSpecialty() { return doctorSpecialty; }
    public void setDoctorSpecialty(String doctorSpecialty) { this.doctorSpecialty = doctorSpecialty; }
    public String getPatientEmail() { return patientEmail; }
    public void setPatientEmail(String patientEmail) { this.patientEmail = patientEmail; }
    public String getPatientPhoneNumber() { return patientPhoneNumber; }
    public void setPatientPhoneNumber(String patientPhoneNumber) { this.patientPhoneNumber = patientPhoneNumber; }
    public LocalDateTime getDecisionTime() { return decisionTime; }
    public void setDecisionTime(LocalDateTime decisionTime) { this.decisionTime = decisionTime; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}
