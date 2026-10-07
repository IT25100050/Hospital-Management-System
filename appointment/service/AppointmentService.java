package com.hospital.hms.appointment.service;

import com.hospital.hms.appointment.dto.AppointmentRequestDTO;
import com.hospital.hms.appointment.dto.AppointmentResponseDTO;
import com.hospital.hms.common.enums.AppointmentStatus;

import java.util.List;

public interface AppointmentService {
    AppointmentResponseDTO createAppointment(AppointmentRequestDTO dto);
    AppointmentResponseDTO getAppointmentById(Long id);
    List<AppointmentResponseDTO> getAllAppointments();
    List<AppointmentResponseDTO> getAppointmentsByPatient(Long patientId);
    List<AppointmentResponseDTO> getAppointmentsByDoctor(Long doctorId);
    AppointmentResponseDTO updateAppointment(Long id, AppointmentRequestDTO dto);
    AppointmentResponseDTO updateAppointmentStatus(Long id, AppointmentStatus status);
    void cancelAppointment(Long id);
    void deleteAppointment(Long id);
}
