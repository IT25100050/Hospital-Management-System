package com.hospital.hms.doctorrecords.controller;

import com.hospital.hms.common.dto.ApiResponse;
import com.hospital.hms.doctorrecords.dto.MedicalRecordDTO;
import com.hospital.hms.doctorrecords.service.MedicalRecordService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medical-records")
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;

    public MedicalRecordController(MedicalRecordService medicalRecordService) {
        this.medicalRecordService = medicalRecordService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MedicalRecordDTO>> createMedicalRecord(@Valid @RequestBody MedicalRecordDTO dto) {
        MedicalRecordDTO response = medicalRecordService.createMedicalRecord(dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Medical record created successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MedicalRecordDTO>> getMedicalRecordById(@PathVariable Long id) {
        MedicalRecordDTO response = medicalRecordService.getMedicalRecordById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Medical record retrieved successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MedicalRecordDTO>>> getAllMedicalRecords() {
        List<MedicalRecordDTO> response = medicalRecordService.getAllMedicalRecords();
        return ResponseEntity.ok(new ApiResponse<>(true, "Medical records retrieved successfully", response));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<MedicalRecordDTO>>> getRecordsByPatient(@PathVariable Long patientId) {
        List<MedicalRecordDTO> response = medicalRecordService.getMedicalRecordsByPatient(patientId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Patient medical records retrieved successfully", response));
    }

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<ApiResponse<List<MedicalRecordDTO>>> getRecordsByDoctor(@PathVariable Long doctorId) {
        List<MedicalRecordDTO> response = medicalRecordService.getMedicalRecordsByDoctor(doctorId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Doctor medical records retrieved successfully", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MedicalRecordDTO>> updateMedicalRecord(@PathVariable Long id, @RequestBody MedicalRecordDTO dto) {
        MedicalRecordDTO response = medicalRecordService.updateMedicalRecord(id, dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Medical record updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMedicalRecord(@PathVariable Long id) {
        medicalRecordService.deleteMedicalRecord(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Medical record deleted successfully", null));
    }
}
