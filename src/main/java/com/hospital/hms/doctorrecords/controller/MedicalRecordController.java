package com.hospital.hms.doctorrecords.controller;

import com.hospital.hms.common.dto.ApiResponse;
import com.hospital.hms.doctorrecords.dto.MedicalRecordCreateRequest;
import com.hospital.hms.doctorrecords.dto.MedicalRecordChangeRequestDTO;
import com.hospital.hms.doctorrecords.dto.MedicalRecordChangeResponseDTO;
import com.hospital.hms.doctorrecords.dto.MedicalRecordChangeReviewDTO;
import com.hospital.hms.doctorrecords.dto.MedicalRecordDTO;
import com.hospital.hms.doctorrecords.service.MedicalRecordService;
import com.hospital.hms.doctorrecords.service.MedicalRecordChangeRequestService;
import com.hospital.hms.doctorrecords.repository.DoctorRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medical-records")
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;
    private final MedicalRecordChangeRequestService changeRequestService;
    private final DoctorRepository doctorRepository;

    public MedicalRecordController(MedicalRecordService medicalRecordService,
                                   MedicalRecordChangeRequestService changeRequestService,
                                   DoctorRepository doctorRepository) {
        this.medicalRecordService = medicalRecordService;
        this.changeRequestService = changeRequestService;
        this.doctorRepository = doctorRepository;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MedicalRecordDTO>> createMedicalRecord(@Valid @RequestBody MedicalRecordDTO dto) {
        MedicalRecordDTO response = medicalRecordService.createMedicalRecord(dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Medical record created successfully", response));
    }

    @PostMapping("/appointment/{appointmentId}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<MedicalRecordDTO>> createForApprovedAppointment(
            @PathVariable Long appointmentId, @Valid @RequestBody MedicalRecordCreateRequest request,
            Authentication authentication) {
        MedicalRecordDTO response = medicalRecordService.createForApprovedAppointment(
                appointmentId, authentication.getName(), request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Medical record created successfully", response));
    }

    @PostMapping("/{id}/change-requests")
    @PreAuthorize("hasRole('DOCTOR_RECORDS_MANAGER')")
    public ResponseEntity<ApiResponse<MedicalRecordChangeResponseDTO>> requestChange(
            @PathVariable Long id, @Valid @RequestBody MedicalRecordChangeRequestDTO request,
            Authentication authentication) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                "Change request sent to the assigned doctor for approval.",
                changeRequestService.submit(id, request, authentication.getName())));
    }

    @GetMapping("/change-requests/mine")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<List<MedicalRecordChangeResponseDTO>>> getMyChangeRequests(
            Authentication authentication) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Medical record change requests retrieved.",
                changeRequestService.listForDoctor(authentication.getName())));
    }

    @PostMapping("/change-requests/{requestId}/review")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<MedicalRecordChangeResponseDTO>> reviewChangeRequest(
            @PathVariable Long requestId, @Valid @RequestBody MedicalRecordChangeReviewDTO review,
            Authentication authentication) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Medical record change request reviewed.",
                changeRequestService.review(requestId, review, authentication.getName())));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<List<MedicalRecordDTO>>> getMyMedicalRecords(Authentication authentication) {
        var doctor = doctorRepository.findByUserUsername(authentication.getName())
                .filter(value -> Boolean.TRUE.equals(value.getActive()))
                .orElseThrow(() -> new com.hospital.hms.common.exception.ResourceNotFoundException("Approved doctor profile not found."));
        return ResponseEntity.ok(new ApiResponse<>(true, "Your medical records retrieved successfully.",
                medicalRecordService.getMedicalRecordsByDoctor(doctor.getId())));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR_RECORDS_MANAGER', 'DOCTOR')")
    public ResponseEntity<ApiResponse<MedicalRecordDTO>> getMedicalRecordById(@PathVariable Long id, Authentication authentication) {
        MedicalRecordDTO response = medicalRecordService.getMedicalRecordById(id);
        enforceDoctor(authentication, response.getDoctorId());
        return ResponseEntity.ok(new ApiResponse<>(true, "Medical record retrieved successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR_RECORDS_MANAGER', 'DOCTOR')")
    public ResponseEntity<ApiResponse<List<MedicalRecordDTO>>> getAllMedicalRecords(Authentication authentication) {
        List<MedicalRecordDTO> response = medicalRecordService.getAllMedicalRecords().stream()
                .filter(record -> hasUnrestrictedRecordAccess(authentication)
                        || isOwnDoctor(authentication, record.getDoctorId())).toList();
        return ResponseEntity.ok(new ApiResponse<>(true, "Medical records retrieved successfully", response));
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR_RECORDS_MANAGER', 'DOCTOR')")
    public ResponseEntity<ApiResponse<List<MedicalRecordDTO>>> getRecordsByPatient(@PathVariable Long patientId,
                                                                                    Authentication authentication) {
        List<MedicalRecordDTO> response = medicalRecordService.getMedicalRecordsByPatient(patientId).stream()
                .filter(record -> hasUnrestrictedRecordAccess(authentication)
                        || isOwnDoctor(authentication, record.getDoctorId())).toList();
        return ResponseEntity.ok(new ApiResponse<>(true, "Patient medical records retrieved successfully", response));
    }

    @GetMapping("/doctor/{doctorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR_RECORDS_MANAGER', 'DOCTOR')")
    public ResponseEntity<ApiResponse<List<MedicalRecordDTO>>> getRecordsByDoctor(@PathVariable Long doctorId,
                                                                                  Authentication authentication) {
        enforceDoctor(authentication, doctorId);
        List<MedicalRecordDTO> response = medicalRecordService.getMedicalRecordsByDoctor(doctorId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Doctor medical records retrieved successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<ApiResponse<MedicalRecordDTO>> updateMedicalRecord(@PathVariable Long id, @RequestBody MedicalRecordDTO dto,
                                                                              Authentication authentication) {
        MedicalRecordDTO response;
        if (isDoctor(authentication)) {
            response = medicalRecordService.updateOwnMedicalRecord(id, dto, authentication.getName());
        } else {
            response = medicalRecordService.updateMedicalRecord(id, dto);
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "Medical record updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR_RECORDS_MANAGER', 'DOCTOR')")
    public ResponseEntity<ApiResponse<Void>> deleteMedicalRecord(@PathVariable Long id, Authentication authentication) {
        if (isDoctor(authentication)) {
            medicalRecordService.deleteOwnMedicalRecord(id, authentication.getName());
        } else if (isManager(authentication)) {
            changeRequestService.deleteRecordAsManager(id, authentication.getName());
        } else {
            medicalRecordService.deleteMedicalRecord(id);
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "Medical record deleted successfully", null));
    }

    private boolean isDoctor(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(authority ->
                authority.getAuthority().equals("ROLE_DOCTOR"));
    }

    private boolean isManager(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(authority ->
                authority.getAuthority().equals("ROLE_DOCTOR_RECORDS_MANAGER"));
    }

    private void enforceDoctor(Authentication authentication, Long doctorId) {
        if (!hasUnrestrictedRecordAccess(authentication) && !isOwnDoctor(authentication, doctorId)) {
            throw new org.springframework.security.access.AccessDeniedException("You can only access medical records assigned to your doctor profile.");
        }
    }

    private boolean hasUnrestrictedRecordAccess(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(authority ->
                authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().equals("ROLE_DOCTOR_RECORDS_MANAGER"));
    }

    private boolean isOwnDoctor(Authentication authentication, Long doctorId) {
        return doctorRepository.findByUserUsername(authentication.getName())
                .filter(doctor -> Boolean.TRUE.equals(doctor.getActive()))
                .map(doctor -> doctor.getId().equals(doctorId)).orElse(false);
    }
}
