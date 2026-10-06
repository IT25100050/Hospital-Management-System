package com.hospital.hms.appointment.service;

import com.hospital.hms.appointment.dto.AppointmentRequestDTO;
import com.hospital.hms.appointment.dto.AppointmentResponseDTO;
import com.hospital.hms.appointment.exception.AppointmentConflictException;
import com.hospital.hms.appointment.model.Appointment;
import com.hospital.hms.appointment.repository.AppointmentRepository;
import com.hospital.hms.common.enums.AppointmentStatus;
import com.hospital.hms.common.exception.ResourceNotFoundException;
import com.hospital.hms.patient.model.Patient;
import com.hospital.hms.patient.repository.PatientRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;

    public AppointmentServiceImpl(AppointmentRepository appointmentRepository, PatientRepository patientRepository) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
    }

    @Override
    public AppointmentResponseDTO createAppointment(AppointmentRequestDTO dto) {
        Patient patient = patientRepository.findById(dto.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + dto.getPatientId()));

        boolean exists = appointmentRepository.existsByDoctorIdAndAppointmentTime(dto.getDoctorId(), dto.getAppointmentTime());
        if (exists) {
            throw new AppointmentConflictException("Doctor already has an appointment scheduled at " + dto.getAppointmentTime());
        }

        Appointment appointment = new Appointment(
                patient,
                dto.getDoctorId(),
                dto.getAppointmentTime(),
                AppointmentStatus.PENDING
        );

        Appointment saved = appointmentRepository.save(appointment);
        return mapToDTO(saved);
    }

    @Override
    public AppointmentResponseDTO getAppointmentById(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));
        return mapToDTO(appointment);
    }

    @Override
    public List<AppointmentResponseDTO> getAllAppointments() {
        return appointmentRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AppointmentResponseDTO> getAppointmentsByPatient(Long patientId) {
        return appointmentRepository.findByPatientId(patientId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AppointmentResponseDTO> getAppointmentsByDoctor(Long doctorId) {
        return appointmentRepository.findByDoctorId(doctorId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public AppointmentResponseDTO updateAppointment(Long id, AppointmentRequestDTO dto) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));

        Long targetDoctorId = dto.getDoctorId() != null ? dto.getDoctorId() : appointment.getDoctorId();
        var targetTime = dto.getAppointmentTime() != null ? dto.getAppointmentTime() : appointment.getAppointmentTime();

        boolean isRescheduling = !targetDoctorId.equals(appointment.getDoctorId())
                || !targetTime.equals(appointment.getAppointmentTime());

        if (isRescheduling && appointmentRepository.existsByDoctorIdAndAppointmentTime(targetDoctorId, targetTime)) {
            throw new AppointmentConflictException("Doctor already has an appointment scheduled at " + targetTime);
        }

        if (dto.getPatientId() != null && !dto.getPatientId().equals(appointment.getPatient().getId())) {
            Patient patient = patientRepository.findById(dto.getPatientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + dto.getPatientId()));
            appointment.setPatient(patient);
        }

        appointment.setDoctorId(targetDoctorId);
        appointment.setAppointmentTime(targetTime);

        Appointment updated = appointmentRepository.save(appointment);
        return mapToDTO(updated);
    }

    @Override
    public AppointmentResponseDTO updateAppointmentStatus(Long id, AppointmentStatus status) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));

        appointment.setStatus(status);
        Appointment updated = appointmentRepository.save(appointment);
        return mapToDTO(updated);
    }

    @Override
    public void cancelAppointment(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);
    }

    @Override
    public void deleteAppointment(Long id) {
        if (!appointmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Appointment not found with id: " + id);
        }
        appointmentRepository.deleteById(id);
    }

    private AppointmentResponseDTO mapToDTO(Appointment appointment) {
        return new AppointmentResponseDTO(
                appointment.getId(),
                appointment.getPatient().getId(),
                appointment.getPatient().getName(),
                appointment.getDoctorId(),
                appointment.getAppointmentTime(),
                appointment.getStatus()
        );
    }
}
