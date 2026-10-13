package com.hospital.hms.doctorrecords.controller;

import com.hospital.hms.common.dto.ApiResponse;
import com.hospital.hms.common.dto.EntityLookupDTO;
import com.hospital.hms.doctorrecords.dto.DoctorDTO;
import com.hospital.hms.doctorrecords.service.DoctorService;
import com.hospital.hms.appointment.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorService doctorService;
    private final AppointmentService appointmentService;

    public DoctorController(DoctorService doctorService, AppointmentService appointmentService) {
        this.doctorService = doctorService;
        this.appointmentService = appointmentService;
    }

    @GetMapping("/available")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<List<com.hospital.hms.doctorrecords.dto.AvailableDoctorDTO>>> getAvailableDoctors(
            @RequestParam LocalDate date) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Available doctors retrieved successfully",
                appointmentService.getAvailableDoctors(date)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR_RECORDS_MANAGER')")
    public ResponseEntity<ApiResponse<DoctorDTO>> createDoctor(@Valid @RequestBody DoctorDTO dto) {
        DoctorDTO response = doctorService.createDoctor(dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Doctor profile created successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR_RECORDS_MANAGER', 'DOCTOR')")
    public ResponseEntity<ApiResponse<DoctorDTO>> getDoctorById(@PathVariable Long id) {
        DoctorDTO response = doctorService.getDoctorById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Doctor retrieved successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR_RECORDS_MANAGER', 'DOCTOR', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<List<DoctorDTO>>> getAllDoctors() {
        List<DoctorDTO> response = doctorService.getAllDoctors();
        return ResponseEntity.ok(new ApiResponse<>(true, "Doctors retrieved successfully", response));
    }

    @GetMapping("/lookup")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<List<EntityLookupDTO>>> getDoctorLookup() {
        List<EntityLookupDTO> response = doctorService.getAllDoctors().stream()
                .map(doctor -> new EntityLookupDTO(doctor.getId(), doctor.getName()))
                .toList();
        return ResponseEntity.ok(new ApiResponse<>(true, "Doctor lookup retrieved successfully", response));
    }

    @GetMapping("/specialization/{specialization}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR_RECORDS_MANAGER', 'DOCTOR')")
    public ResponseEntity<ApiResponse<List<DoctorDTO>>> getDoctorsBySpecialization(@PathVariable String specialization) {
        List<DoctorDTO> response = doctorService.getDoctorsBySpecialization(specialization);
        return ResponseEntity.ok(new ApiResponse<>(true, "Doctors retrieved successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR_RECORDS_MANAGER')")
    public ResponseEntity<ApiResponse<DoctorDTO>> updateDoctor(@PathVariable Long id, @RequestBody DoctorDTO dto) {
        DoctorDTO response = doctorService.updateDoctor(id, dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Doctor profile updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR_RECORDS_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteDoctor(@PathVariable Long id) {
        doctorService.deleteDoctor(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Doctor deleted successfully", null));
    }
}
