package com.hospital.hms.appointment.controller;

import com.hospital.hms.appointment.dto.AppointmentRequestDTO;
import com.hospital.hms.appointment.dto.AppointmentResponseDTO;
import com.hospital.hms.appointment.service.AppointmentService;
import com.hospital.hms.common.dto.ApiResponse;
import com.hospital.hms.common.enums.AppointmentStatus;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<List<AppointmentResponseDTO>>> getMine(Authentication authentication) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Your appointments retrieved successfully",
                appointmentService.getMyAppointments(authentication.getName())));
    }

    @GetMapping("/doctor/mine")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<List<AppointmentResponseDTO>>> getDoctorAppointments(Authentication authentication) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Doctor appointments retrieved successfully",
                appointmentService.getMyDoctorAppointments(authentication.getName())));
    }

    @PostMapping("/doctor/{id}/decision")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<AppointmentResponseDTO>> decideAppointment(
            @PathVariable Long id, @Valid @RequestBody AppointmentDecisionRequest request, Authentication authentication) {
        AppointmentResponseDTO response = appointmentService.decideAppointment(
                id, authentication.getName(), request.getStatus(), request.getRejectionReason());
        return ResponseEntity.ok(new ApiResponse<>(true, "Appointment decision saved", response));
    }

    @PostMapping("/mine/{id}/cancel")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<Void>> cancelMine(@PathVariable Long id, Authentication authentication) {
        appointmentService.cancelMyAppointment(id, authentication.getName());
        return ResponseEntity.ok(new ApiResponse<>(true, "Appointment cancelled successfully", null));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PATIENT', 'RECEPTIONIST', 'ADMIN')")
    public ResponseEntity<ApiResponse<AppointmentResponseDTO>> createAppointment(
            @Valid @RequestBody AppointmentRequestDTO dto, Authentication authentication) {
        AppointmentResponseDTO response = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_PATIENT"))
                ? appointmentService.createPatientAppointment(dto, authentication.getName())
                : appointmentService.createAppointment(dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Appointment booked successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<AppointmentResponseDTO>> getAppointmentById(@PathVariable Long id) {
        AppointmentResponseDTO response = appointmentService.getAppointmentById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Appointment retrieved successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<List<AppointmentResponseDTO>>> getAllAppointments() {
        List<AppointmentResponseDTO> response = appointmentService.getAllAppointments();
        return ResponseEntity.ok(new ApiResponse<>(true, "Appointments retrieved successfully", response));
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<List<AppointmentResponseDTO>>> getAppointmentsByPatient(@PathVariable Long patientId) {
        List<AppointmentResponseDTO> response = appointmentService.getAppointmentsByPatient(patientId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Patient appointments retrieved successfully", response));
    }

    @GetMapping("/doctor/{doctorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<List<AppointmentResponseDTO>>> getAppointmentsByDoctor(@PathVariable Long doctorId) {
        List<AppointmentResponseDTO> response = appointmentService.getAppointmentsByDoctor(doctorId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Doctor appointments retrieved successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<AppointmentResponseDTO>> updateAppointment(
            @PathVariable Long id, @RequestBody AppointmentRequestDTO dto) {
        AppointmentResponseDTO response = appointmentService.updateAppointment(id, dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Appointment updated successfully", response));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<AppointmentResponseDTO>> updateStatus(@PathVariable Long id, @RequestParam AppointmentStatus status) {
        AppointmentResponseDTO response = appointmentService.updateAppointmentStatus(id, status);
        return ResponseEntity.ok(new ApiResponse<>(true, "Appointment status updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<Void>> cancelAppointment(@PathVariable Long id) {
        appointmentService.cancelAppointment(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Appointment cancelled successfully", null));
    }

    @DeleteMapping("/{id}/permanent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteAppointment(@PathVariable Long id) {
        appointmentService.deleteAppointment(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Appointment permanently deleted successfully", null));
    }
}
