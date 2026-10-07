package com.hospital.hms.patient.controller;

import com.hospital.hms.common.dto.ApiResponse;
import com.hospital.hms.common.dto.EntityLookupDTO;
import com.hospital.hms.patient.dto.PatientRequestDTO;
import com.hospital.hms.patient.dto.PatientResponseDTO;
import com.hospital.hms.patient.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<PatientResponseDTO>> createPatient(@Valid @RequestBody PatientRequestDTO dto) {
        PatientResponseDTO response = patientService.createPatient(dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Patient created successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<PatientResponseDTO>> getPatient(@PathVariable Long id) {
        PatientResponseDTO response = patientService.getPatientById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Patient retrieved successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'DOCTOR_RECORDS_MANAGER')")
    public ResponseEntity<ApiResponse<List<PatientResponseDTO>>> getAllPatients() {
        List<PatientResponseDTO> response = patientService.getAllPatients();
        return ResponseEntity.ok(new ApiResponse<>(true, "Patients retrieved successfully", response));
    }

    @GetMapping("/lookup")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'FINANCE_OFFICER')")
    public ResponseEntity<ApiResponse<List<EntityLookupDTO>>> getPatientLookup() {
        List<EntityLookupDTO> response = patientService.getAllPatients().stream()
                .map(patient -> new EntityLookupDTO(patient.getId(), patient.getName()))
                .toList();
        return ResponseEntity.ok(new ApiResponse<>(true, "Patient lookup retrieved successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<PatientResponseDTO>> updatePatient(
            @PathVariable Long id, @Valid @RequestBody PatientRequestDTO dto) {
        PatientResponseDTO response = patientService.updatePatient(id, dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Patient updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePatient(@PathVariable Long id) {
        patientService.deletePatient(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Patient deleted successfully", null));
    }
}
