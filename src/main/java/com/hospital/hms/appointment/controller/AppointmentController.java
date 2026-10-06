package com.hospital.hms.appointment.controller;

import com.hospital.hms.appointment.dto.AppointmentRequestDTO;
import com.hospital.hms.appointment.dto.AppointmentResponseDTO;
import com.hospital.hms.appointment.service.AppointmentService;
import com.hospital.hms.common.dto.ApiResponse;
import com.hospital.hms.common.enums.AppointmentStatus;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AppointmentResponseDTO>> createAppointment(
            @Valid @RequestBody AppointmentRequestDTO dto) {
        AppointmentResponseDTO response = appointmentService.createAppointment(dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Appointment booked successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AppointmentResponseDTO>> getAppointmentById(@PathVariable Long id) {
        AppointmentResponseDTO response = appointmentService.getAppointmentById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Appointment retrieved successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AppointmentResponseDTO>>> getAllAppointments() {
        List<AppointmentResponseDTO> response = appointmentService.getAllAppointments();
        return ResponseEntity.ok(new ApiResponse<>(true, "Appointments retrieved successfully", response));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<AppointmentResponseDTO>>> getAppointmentsByPatient(@PathVariable Long patientId) {
        List<AppointmentResponseDTO> response = appointmentService.getAppointmentsByPatient(patientId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Patient appointments retrieved successfully", response));
    }

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<ApiResponse<List<AppointmentResponseDTO>>> getAppointmentsByDoctor(@PathVariable Long doctorId) {
        List<AppointmentResponseDTO> response = appointmentService.getAppointmentsByDoctor(doctorId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Doctor appointments retrieved successfully", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AppointmentResponseDTO>> updateAppointment(
            @PathVariable Long id, @RequestBody AppointmentRequestDTO dto) {
        AppointmentResponseDTO response = appointmentService.updateAppointment(id, dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Appointment updated successfully", response));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<AppointmentResponseDTO>> updateStatus(@PathVariable Long id, @RequestParam AppointmentStatus status) {
        AppointmentResponseDTO response = appointmentService.updateAppointmentStatus(id, status);
        return ResponseEntity.ok(new ApiResponse<>(true, "Appointment status updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> cancelAppointment(@PathVariable Long id) {
        appointmentService.cancelAppointment(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Appointment cancelled successfully", null));
    }

    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<ApiResponse<Void>> deleteAppointment(@PathVariable Long id) {
        appointmentService.deleteAppointment(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Appointment permanently deleted successfully", null));
    }
}
