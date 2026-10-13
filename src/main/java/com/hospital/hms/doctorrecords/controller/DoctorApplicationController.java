package com.hospital.hms.doctorrecords.controller;

import com.hospital.hms.common.dto.ApiResponse;
import com.hospital.hms.doctorrecords.dto.*;
import com.hospital.hms.doctorrecords.service.DoctorApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctor-applications")
public class DoctorApplicationController {
    private final DoctorApplicationService service;

    public DoctorApplicationController(DoctorApplicationService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<ApiResponse<DoctorApplicationResponse>> submit(
            @Valid @RequestBody DoctorApplicationRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                "Your doctor registration is pending approval.", service.submit(request)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR_RECORDS_MANAGER')")
    public ResponseEntity<ApiResponse<List<DoctorApplicationResponse>>> list() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Doctor registration requests retrieved.",
                service.list()));
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR_RECORDS_MANAGER')")
    public ResponseEntity<ApiResponse<DoctorApplicationResponse>> review(
            @PathVariable Long id, @Valid @RequestBody DoctorApplicationReviewRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Doctor registration request reviewed.",
                service.review(id, request, authentication.getName())));
    }
}
