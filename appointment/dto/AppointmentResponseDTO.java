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

    public AppointmentResponseDTO() {}

    public AppointmentResponseDTO(Long id, Long patientId, String patientName, Long doctorId, LocalDateTime appointmentTime, AppointmentStatus status) {
        this.id = id;
        this.patientId = patientId;
        this.patientName = patientName;
        this.doctorId = doctorId;
        this.appointmentTime = appointmentTime;
        this.status = status;
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
}
