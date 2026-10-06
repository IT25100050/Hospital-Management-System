package com.hospital.hms.pharmacy.controller;

import com.hospital.hms.common.dto.ApiResponse;
import com.hospital.hms.pharmacy.dto.PrescriptionDTO;
import com.hospital.hms.pharmacy.service.PrescriptionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prescriptions")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    public PrescriptionController(PrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PrescriptionDTO>> createPrescription(@Valid @RequestBody PrescriptionDTO dto) {
        PrescriptionDTO response = prescriptionService.createPrescription(dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Prescription created successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PrescriptionDTO>> getPrescriptionById(@PathVariable Long id) {
        PrescriptionDTO response = prescriptionService.getPrescriptionById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Prescription retrieved successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PrescriptionDTO>>> getAllPrescriptions() {
        List<PrescriptionDTO> response = prescriptionService.getAllPrescriptions();
        return ResponseEntity.ok(new ApiResponse<>(true, "Prescriptions retrieved successfully", response));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<PrescriptionDTO>>> getPrescriptionsByPatient(@PathVariable Long patientId) {
        List<PrescriptionDTO> response = prescriptionService.getPrescriptionsByPatient(patientId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Patient prescriptions retrieved successfully", response));
    }

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<ApiResponse<List<PrescriptionDTO>>> getPrescriptionsByDoctor(@PathVariable Long doctorId) {
        List<PrescriptionDTO> response = prescriptionService.getPrescriptionsByDoctor(doctorId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Doctor prescriptions retrieved successfully", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PrescriptionDTO>> updatePrescription(@PathVariable Long id, @RequestBody PrescriptionDTO dto) {
        PrescriptionDTO response = prescriptionService.updatePrescription(id, dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Prescription updated successfully", response));
    }

    @PatchMapping("/{id}/fulfill")
    public ResponseEntity<ApiResponse<Void>> fulfillPrescription(@PathVariable Long id) {
        prescriptionService.fulfillPrescription(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Prescription fulfilled and stock updated successfully", null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePrescription(@PathVariable Long id) {
        prescriptionService.deletePrescription(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Prescription deleted successfully", null));
    }
}
