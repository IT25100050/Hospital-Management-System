package com.hospital.hms.doctorrecords.controller;

import com.hospital.hms.common.dto.ApiResponse;
import com.hospital.hms.doctorrecords.dto.DoctorDTO;
import com.hospital.hms.doctorrecords.service.DoctorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DoctorDTO>> createDoctor(@Valid @RequestBody DoctorDTO dto) {
        DoctorDTO response = doctorService.createDoctor(dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Doctor profile created successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DoctorDTO>> getDoctorById(@PathVariable Long id) {
        DoctorDTO response = doctorService.getDoctorById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Doctor retrieved successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DoctorDTO>>> getAllDoctors() {
        List<DoctorDTO> response = doctorService.getAllDoctors();
        return ResponseEntity.ok(new ApiResponse<>(true, "Doctors retrieved successfully", response));
    }

    @GetMapping("/specialization/{specialization}")
    public ResponseEntity<ApiResponse<List<DoctorDTO>>> getDoctorsBySpecialization(@PathVariable String specialization) {
        List<DoctorDTO> response = doctorService.getDoctorsBySpecialization(specialization);
        return ResponseEntity.ok(new ApiResponse<>(true, "Doctors retrieved successfully", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DoctorDTO>> updateDoctor(@PathVariable Long id, @RequestBody DoctorDTO dto) {
        DoctorDTO response = doctorService.updateDoctor(id, dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Doctor profile updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDoctor(@PathVariable Long id) {
        doctorService.deleteDoctor(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Doctor deleted successfully", null));
    }
}
