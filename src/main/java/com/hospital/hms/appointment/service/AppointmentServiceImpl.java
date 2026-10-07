package com.hospital.hms.appointment.service;

import com.hospital.hms.appointment.dto.AppointmentRequestDTO;
import com.hospital.hms.appointment.dto.AppointmentResponseDTO;
import com.hospital.hms.appointment.exception.AppointmentConflictException;
import com.hospital.hms.appointment.model.Appointment;
import com.hospital.hms.appointment.repository.AppointmentRepository;
import com.hospital.hms.common.enums.AppointmentStatus;
import com.hospital.hms.common.exception.BadRequestException;
import com.hospital.hms.common.exception.ResourceNotFoundException;
import com.hospital.hms.doctorrecords.dto.AvailableDoctorDTO;
import com.hospital.hms.doctorrecords.model.Doctor;
import com.hospital.hms.doctorrecords.repository.DoctorRepository;
import com.hospital.hms.patient.model.Patient;
import com.hospital.hms.patient.service.PatientProfileService;
import com.hospital.hms.patient.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class AppointmentServiceImpl implements AppointmentService {
    private static final Set<AppointmentStatus> BOOKING_STATUSES =
            EnumSet.of(AppointmentStatus.PENDING, AppointmentStatus.APPROVED);
    private static final Set<AppointmentStatus> APPROVED_BOOKING_STATUSES =
            EnumSet.of(AppointmentStatus.APPROVED);

    private final AppointmentRepository appointmentRepository;
    private final PatientProfileService patientProfileService;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    public AppointmentServiceImpl(AppointmentRepository appointmentRepository, PatientProfileService patientProfileService,
                                  PatientRepository patientRepository,
                                  DoctorRepository doctorRepository) {
        this.appointmentRepository = appointmentRepository;
        this.patientProfileService = patientProfileService;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
    }

    @Override
    @Transactional
    public AppointmentResponseDTO createPatientAppointment(AppointmentRequestDTO dto, String username) {
        Patient patient = patientProfileService.getOrCreateForUser(username);
        return createBooking(patient, dto);
    }

    @Override
    @Transactional
    public AppointmentResponseDTO createAppointment(AppointmentRequestDTO dto) {
        if (dto.getPatientId() == null) {
            throw new BadRequestException("Patient ID is required for staff bookings.");
        }
        Patient patient = patientRepository.findById(dto.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found."));
        return createBooking(patient, dto);
    }

    private AppointmentResponseDTO createBooking(Patient patient, AppointmentRequestDTO dto) {
        validateSlot(dto.getAppointmentTime());
        Doctor doctor = lockActiveDoctor(dto.getDoctorId());
        LocalDateTime time = dto.getAppointmentTime().withSecond(0).withNano(0);
        if (appointmentRepository.existsByPatientIdAndDoctorIdAndAppointmentTimeAndStatusIn(
                patient.getId(), doctor.getId(), time, BOOKING_STATUSES)) {
            throw new AppointmentConflictException("You already have an active booking for this doctor and time.");
        }
        if (appointmentRepository.existsByDoctorIdAndAppointmentTimeAndStatusIn(
                doctor.getId(), time, BOOKING_STATUSES)) {
            throw new AppointmentConflictException("This consultation slot is no longer available.");
        }
        Appointment appointment = new Appointment(patient, doctor.getId(), time, dto.getReason().trim(),
                AppointmentStatus.PENDING);
        return mapToDTO(appointmentRepository.save(appointment));
    }

    @Override
    @Transactional
    public List<AppointmentResponseDTO> getMyAppointments(String username) {
        Patient patient = patientProfileService.getOrCreateForUser(username);
        return appointmentRepository.findByPatientId(patient.getId()).stream().map(this::mapToDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponseDTO> getMyDoctorAppointments(String username) {
        Doctor doctor = doctorRepository.findByUserUsername(username)
                .filter(value -> Boolean.TRUE.equals(value.getActive()))
                .orElseThrow(() -> new ResourceNotFoundException("Approved doctor profile not found."));
        return appointmentRepository.findByDoctorId(doctor.getId()).stream().map(this::mapToDTO).toList();
    }

    @Override
    @Transactional
    public AppointmentResponseDTO decideAppointment(Long id, String username, AppointmentStatus status,
                                                     String rejectionReason) {
        if (status != AppointmentStatus.APPROVED && status != AppointmentStatus.REJECTED) {
            throw new BadRequestException("A pending appointment can only be approved or rejected.");
        }
        Doctor doctor = doctorRepository.findByUserUsername(username)
                .filter(value -> Boolean.TRUE.equals(value.getActive()))
                .orElseThrow(() -> new ResourceNotFoundException("Approved doctor profile not found."));
        lockActiveDoctor(doctor.getId());
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found."));
        if (!doctor.getId().equals(appointment.getDoctorId())) {
            throw new ResourceNotFoundException("Appointment not found.");
        }
        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new BadRequestException("Only pending appointments can be approved or rejected.");
        }
        if (status == AppointmentStatus.APPROVED
                && appointmentRepository.existsByDoctorIdAndAppointmentTimeAndStatusIn(
                doctor.getId(), appointment.getAppointmentTime(), APPROVED_BOOKING_STATUSES)) {
            throw new AppointmentConflictException("Another appointment has already been approved for this time.");
        }
        appointment.setStatus(status);
        appointment.setDecisionTime(LocalDateTime.now());
        appointment.setRejectionReason(status == AppointmentStatus.REJECTED
                && rejectionReason != null && !rejectionReason.isBlank() ? rejectionReason.trim() : null);
        return mapToDTO(appointmentRepository.save(appointment));
    }

    @Override
    @Transactional
    public void cancelMyAppointment(Long id, String username) {
        Patient patient = patientProfileService.getOrCreateForUser(username);
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found."));
        if (!patient.getId().equals(appointment.getPatient().getId())) {
            throw new ResourceNotFoundException("Appointment not found.");
        }
        if (appointment.getStatus() != AppointmentStatus.PENDING
                && appointment.getStatus() != AppointmentStatus.APPROVED) {
            throw new BadRequestException("Only pending or approved appointments can be cancelled.");
        }
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setDecisionTime(LocalDateTime.now());
        appointmentRepository.save(appointment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailableDoctorDTO> getAvailableDoctors(LocalDate date) {
        if (date == null || !date.isAfter(LocalDate.now())) {
            throw new BadRequestException("Choose a future consultation date.");
        }
        List<Doctor> doctors = doctorRepository.findApprovedDoctors();
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay().minusNanos(1);
        return doctors.stream().map(doctor -> {
            Set<LocalDateTime> booked = appointmentRepository
                    .findByDoctorIdAndAppointmentTimeBetweenAndStatusIn(
                            doctor.getId(), start, end, BOOKING_STATUSES)
                    .stream().map(Appointment::getAppointmentTime).collect(java.util.stream.Collectors.toSet());
            List<String> slots = date.getDayOfWeek() == DayOfWeek.SATURDAY
                    || date.getDayOfWeek() == DayOfWeek.SUNDAY ? List.of()
                    : generateSlots(date).stream().filter(slot -> slot.isAfter(LocalDateTime.now()))
                    .filter(slot -> !booked.contains(slot)).map(LocalDateTime::toString).toList();
            return new AvailableDoctorDTO(doctor, slots);
        }).toList();
    }

    private List<LocalDateTime> generateSlots(LocalDate date) {
        return java.util.stream.IntStream.range(0, 16)
                .mapToObj(index -> date.atTime(9, 0).plusMinutes(index * 30L)).toList();
    }

    private Doctor lockActiveDoctor(Long doctorId) {
        if (doctorId == null) throw new BadRequestException("Doctor is required.");
        Doctor doctor = doctorRepository.findByIdForUpdate(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found."));
        if (!Boolean.TRUE.equals(doctor.getActive())) {
            throw new BadRequestException("This doctor is not approved for appointments.");
        }
        return doctor;
    }

    private void validateSlot(LocalDateTime time) {
        if (time == null || !time.isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Appointment date and time must be in the future.");
        }
        if (time.getDayOfWeek() == DayOfWeek.SATURDAY || time.getDayOfWeek() == DayOfWeek.SUNDAY
                || time.toLocalTime().isBefore(LocalTime.of(9, 0))
                || !time.toLocalTime().isBefore(LocalTime.of(17, 0))
                || time.getMinute() % 30 != 0 || time.getSecond() != 0 || time.getNano() != 0) {
            throw new BadRequestException("Select an available weekday slot between 09:00 and 16:30.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponseDTO getAppointmentById(Long id) {
        return appointmentRepository.findById(id).map(this::mapToDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponseDTO> getAllAppointments() {
        return appointmentRepository.findAll().stream().map(this::mapToDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponseDTO> getAppointmentsByPatient(Long patientId) {
        return appointmentRepository.findByPatientId(patientId).stream().map(this::mapToDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponseDTO> getAppointmentsByDoctor(Long doctorId) {
        return appointmentRepository.findByDoctorId(doctorId).stream().map(this::mapToDTO).toList();
    }

    @Override
    @Transactional
    public AppointmentResponseDTO updateAppointment(Long id, AppointmentRequestDTO dto) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));
        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new BadRequestException("Only pending appointments can be edited.");
        }
        Long doctorId = dto.getDoctorId() == null ? appointment.getDoctorId() : dto.getDoctorId();
        LocalDateTime time = dto.getAppointmentTime() == null ? appointment.getAppointmentTime() : dto.getAppointmentTime();
        validateSlot(time);
        Doctor doctor = lockActiveDoctor(doctorId);
        if ((!doctorId.equals(appointment.getDoctorId()) || !time.equals(appointment.getAppointmentTime()))
                && appointmentRepository.existsByDoctorIdAndAppointmentTimeAndStatusIn(
                doctor.getId(), time, BOOKING_STATUSES)) {
            throw new AppointmentConflictException("This consultation slot is no longer available.");
        }
        if (dto.getPatientId() != null && !dto.getPatientId().equals(appointment.getPatient().getId())) {
            Patient patient = patientRepository.findById(dto.getPatientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient not found."));
            appointment.setPatient(patient);
        }
        appointment.setDoctorId(doctorId);
        appointment.setAppointmentTime(time);
        if (dto.getReason() != null) appointment.setReason(dto.getReason().trim());
        return mapToDTO(appointmentRepository.save(appointment));
    }

    @Override
    @Transactional
    public AppointmentResponseDTO updateAppointmentStatus(Long id, AppointmentStatus status) {
        throw new BadRequestException("Use the assigned doctor's approval action or cancel the booking.");
    }

    @Override
    @Transactional
    public void cancelAppointment(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));
        if (appointment.getStatus() != AppointmentStatus.PENDING
                && appointment.getStatus() != AppointmentStatus.APPROVED) {
            throw new BadRequestException("Only pending or approved appointments can be cancelled.");
        }
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setDecisionTime(LocalDateTime.now());
        appointmentRepository.save(appointment);
    }

    @Override
    @Transactional
    public void deleteAppointment(Long id) {
        if (!appointmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Appointment not found with id: " + id);
        }
        appointmentRepository.deleteById(id);
    }

    private AppointmentResponseDTO mapToDTO(Appointment appointment) {
        Doctor doctor = doctorRepository.findById(appointment.getDoctorId()).orElse(null);
        return new AppointmentResponseDTO(
                appointment.getId(), appointment.getPatient().getId(), appointment.getPatient().getName(),
                appointment.getDoctorId(), appointment.getAppointmentTime(), appointment.getStatus(),
                appointment.getReason(), doctor == null ? null : doctor.getName(),
                doctor == null ? null : doctor.getSpecialization(), appointment.getPatient().getEmail(),
                appointment.getPatient().getPhoneNumber(), appointment.getDecisionTime(),
                appointment.getRejectionReason());
    }
}
