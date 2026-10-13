package com.hospital.hms.appointment.service;

import com.hospital.hms.appointment.dto.AppointmentRequestDTO;
import com.hospital.hms.appointment.exception.AppointmentConflictException;
import com.hospital.hms.appointment.model.Appointment;
import com.hospital.hms.appointment.repository.AppointmentRepository;
import com.hospital.hms.common.enums.AppointmentStatus;
import com.hospital.hms.doctorrecords.model.Doctor;
import com.hospital.hms.doctorrecords.repository.DoctorRepository;
import com.hospital.hms.patient.model.Patient;
import com.hospital.hms.patient.repository.PatientRepository;
import com.hospital.hms.patient.service.PatientProfileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private PatientProfileService patientProfileService;
    @Mock private PatientRepository patientRepository;
    @Mock private DoctorRepository doctorRepository;
    @InjectMocks private AppointmentServiceImpl service;

    @Test
    void patientBookingUsesAuthenticatedPatientAndStartsPending() {
        Patient authenticatedPatient = mock(Patient.class);
        Doctor selectedDoctor = mock(Doctor.class);
        when(authenticatedPatient.getId()).thenReturn(14L);
        when(selectedDoctor.getId()).thenReturn(22L);
        when(selectedDoctor.getActive()).thenReturn(true);
        when(patientProfileService.getOrCreateForUser("patient-login")).thenReturn(authenticatedPatient);
        when(doctorRepository.findByIdForUpdate(22L)).thenReturn(Optional.of(selectedDoctor));
        when(doctorRepository.findById(22L)).thenReturn(Optional.of(selectedDoctor));
        when(appointmentRepository.existsByPatientIdAndDoctorIdAndAppointmentTimeAndStatusIn(
                eq(14L), eq(22L), any(LocalDateTime.class), anyCollection())).thenReturn(false);
        when(appointmentRepository.existsByDoctorIdAndAppointmentTimeAndStatusIn(
                eq(22L), any(LocalDateTime.class), anyCollection())).thenReturn(false);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentRequestDTO request = new AppointmentRequestDTO(999L, 22L, futureSlot());
        request.setReason("Annual check-up");
        var result = service.createPatientAppointment(request, "patient-login");

        ArgumentCaptor<Appointment> appointmentCaptor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository).save(appointmentCaptor.capture());
        assertEquals(AppointmentStatus.PENDING, result.getStatus());
        assertEquals(authenticatedPatient, appointmentCaptor.getValue().getPatient());
        assertEquals(AppointmentStatus.PENDING, appointmentCaptor.getValue().getStatus());
        assertEquals("Annual check-up", appointmentCaptor.getValue().getReason());
        verify(patientProfileService).getOrCreateForUser("patient-login");
    }

    @Test
    void conflictingSlotIsRejectedBeforeSaving() {
        Patient patient = mock(Patient.class);
        Doctor doctor = mock(Doctor.class);
        when(patient.getId()).thenReturn(14L);
        when(doctor.getId()).thenReturn(22L);
        when(doctor.getActive()).thenReturn(true);
        when(patientProfileService.getOrCreateForUser("patient-login")).thenReturn(patient);
        when(doctorRepository.findByIdForUpdate(22L)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.existsByPatientIdAndDoctorIdAndAppointmentTimeAndStatusIn(
                eq(14L), eq(22L), any(LocalDateTime.class), anyCollection())).thenReturn(false);
        when(appointmentRepository.existsByDoctorIdAndAppointmentTimeAndStatusIn(
                eq(22L), any(LocalDateTime.class), anyCollection())).thenReturn(true);

        AppointmentRequestDTO request = new AppointmentRequestDTO(null, 22L, futureSlot());
        request.setReason("Consultation");
        assertThrows(AppointmentConflictException.class,
                () -> service.createPatientAppointment(request, "patient-login"));
        verify(appointmentRepository, never()).save(any());
    }

    private LocalDateTime futureSlot() {
        LocalDate date = LocalDate.now().plusDays(1);
        while (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        LocalDateTime slot = date.atTime(10, 0);
        return slot.isAfter(LocalDateTime.now()) ? slot : LocalDate.now().plusWeeks(1).atTime(LocalTime.of(10, 0));
    }
}
