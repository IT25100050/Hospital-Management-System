package com.hospital.hms.appointment.model;

import com.hospital.hms.common.enums.AppointmentStatus;
import com.hospital.hms.patient.model.Patient;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    private Long doctorId;
    private LocalDateTime appointmentTime;

    @Enumerated(EnumType.STRING)
    private AppointmentStatus status;

    public Appointment() {}

    public Appointment(Patient patient, Long doctorId, LocalDateTime appointmentTime, AppointmentStatus status) {
        this.patient = patient;
        this.doctorId = doctorId;
        this.appointmentTime = appointmentTime;
        this.status = status;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }
    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }
    public LocalDateTime getAppointmentTime() { return appointmentTime; }
    public void setAppointmentTime(LocalDateTime appointmentTime) { this.appointmentTime = appointmentTime; }
    public AppointmentStatus getStatus() { return status; }
    public void setStatus(AppointmentStatus status) { this.status = status; }
}
