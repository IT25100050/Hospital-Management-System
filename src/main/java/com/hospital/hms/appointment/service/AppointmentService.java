package com.hospital.hms.appointment.service;

import com.hospital.hms.appointment.dto.AppointmentRequestDTO;
import com.hospital.hms.appointment.dto.AppointmentResponseDTO;
import com.hospital.hms.common.enums.AppointmentStatus;

import java.util.List;
import java.time.LocalDate;
import com.hospital.hms.doctorrecords.dto.AvailableDoctorDTO;

public interface AppointmentService {
    AppointmentResponseDTO createAppointment(AppointmentRequestDTO dto);
    AppointmentResponseDTO createPatientAppointment(AppointmentRequestDTO dto, String username);
    List<AppointmentResponseDTO> getMyAppointments(String username);
    List<AppointmentResponseDTO> getMyDoctorAppointments(String username);
    AppointmentResponseDTO decideAppointment(Long id, String username, AppointmentStatus status, String rejectionReason);
    void cancelMyAppointment(Long id, String username);
    List<AvailableDoctorDTO> getAvailableDoctors(LocalDate date);
    AppointmentResponseDTO getAppointmentById(Long id);
    List<AppointmentResponseDTO> getAllAppointments();
    List<AppointmentResponseDTO> getAppointmentsByPatient(Long patientId);
    List<AppointmentResponseDTO> getAppointmentsByDoctor(Long doctorId);
    AppointmentResponseDTO updateAppointment(Long id, AppointmentRequestDTO dto);
    AppointmentResponseDTO updateAppointmentStatus(Long id, AppointmentStatus status);
    void cancelAppointment(Long id);
    void deleteAppointment(Long id);
}
