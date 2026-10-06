package com.hospital.hms.laboratory.controller;

import com.hospital.hms.common.dto.ApiResponse;
import com.hospital.hms.common.enums.TestStatus;
import com.hospital.hms.laboratory.dto.LabReportDTO;
import com.hospital.hms.laboratory.dto.LabTestDTO;
import com.hospital.hms.laboratory.service.LabTestService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lab")
public class LabTestController {

    private final LabTestService labTestService;

    public LabTestController(LabTestService labTestService) {
        this.labTestService = labTestService;
    }

    @PostMapping("/tests")
    public ResponseEntity<ApiResponse<LabTestDTO>> orderLabTest(@Valid @RequestBody LabTestDTO dto) {
        LabTestDTO response = labTestService.orderLabTest(dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Lab test ordered successfully", response));
    }

    @GetMapping("/tests/{id}")
    public ResponseEntity<ApiResponse<LabTestDTO>> getLabTestById(@PathVariable Long id) {
        LabTestDTO response = labTestService.getLabTestById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Lab test retrieved successfully", response));
    }

    @GetMapping("/tests")
    public ResponseEntity<ApiResponse<List<LabTestDTO>>> getAllLabTests() {
        List<LabTestDTO> response = labTestService.getAllLabTests();
        return ResponseEntity.ok(new ApiResponse<>(true, "Lab tests retrieved successfully", response));
    }

    @GetMapping("/tests/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<LabTestDTO>>> getLabTestsByPatient(@PathVariable Long patientId) {
        List<LabTestDTO> response = labTestService.getLabTestsByPatient(patientId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Patient lab tests retrieved successfully", response));
    }

    @PutMapping("/tests/{id}")
    public ResponseEntity<ApiResponse<LabTestDTO>> updateLabTest(@PathVariable Long id, @RequestBody LabTestDTO dto) {
        LabTestDTO response = labTestService.updateLabTest(id, dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Lab test updated successfully", response));
    }

    @PatchMapping("/tests/{id}/status")
    public ResponseEntity<ApiResponse<LabTestDTO>> updateStatus(@PathVariable Long id, @RequestParam TestStatus status) {
        LabTestDTO response = labTestService.updateTestStatus(id, status);
        return ResponseEntity.ok(new ApiResponse<>(true, "Lab test status updated successfully", response));
    }

    @DeleteMapping("/tests/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteLabTest(@PathVariable Long id) {
        labTestService.deleteLabTest(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Lab test deleted successfully", null));
    }

    @PostMapping("/reports")
    public ResponseEntity<ApiResponse<LabReportDTO>> createReport(
            @RequestParam Long testId,
            @RequestParam String resultDetails,
            @RequestParam(required = false) String remarks) {
        LabReportDTO response = labTestService.createLabReport(testId, resultDetails, remarks);
        return ResponseEntity.ok(new ApiResponse<>(true, "Lab report generated successfully", response));
    }

    @GetMapping("/reports/test/{testId}")
    public ResponseEntity<ApiResponse<LabReportDTO>> getReportByTestId(@PathVariable Long testId) {
        LabReportDTO response = labTestService.getReportByTestId(testId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Lab report retrieved successfully", response));
    }

    @PutMapping("/reports/{reportId}")
    public ResponseEntity<ApiResponse<LabReportDTO>> updateReport(
            @PathVariable Long reportId,
            @RequestParam(required = false) String resultDetails,
            @RequestParam(required = false) String remarks) {
        LabReportDTO response = labTestService.updateLabReport(reportId, resultDetails, remarks);
        return ResponseEntity.ok(new ApiResponse<>(true, "Lab report updated successfully", response));
    }

    @DeleteMapping("/reports/{reportId}")
    public ResponseEntity<ApiResponse<Void>> deleteReport(@PathVariable Long reportId) {
        labTestService.deleteLabReport(reportId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Lab report deleted successfully", null));
    }
}
