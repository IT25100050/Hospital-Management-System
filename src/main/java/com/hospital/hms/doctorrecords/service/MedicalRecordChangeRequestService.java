package com.hospital.hms.doctorrecords.service;

import com.hospital.hms.auth.model.User;
import com.hospital.hms.auth.repository.UserRepository;
import com.hospital.hms.common.enums.UserRole;
import com.hospital.hms.common.exception.BadRequestException;
import com.hospital.hms.common.exception.ResourceNotFoundException;
import com.hospital.hms.doctorrecords.dto.MedicalRecordChangeRequestDTO;
import com.hospital.hms.doctorrecords.dto.MedicalRecordChangeResponseDTO;
import com.hospital.hms.doctorrecords.dto.MedicalRecordChangeReviewDTO;
import com.hospital.hms.doctorrecords.dto.MedicalRecordDTO;
import com.hospital.hms.doctorrecords.model.*;
import com.hospital.hms.doctorrecords.repository.DoctorRepository;
import com.hospital.hms.doctorrecords.repository.MedicalRecordChangeRequestRepository;
import com.hospital.hms.doctorrecords.repository.MedicalRecordRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MedicalRecordChangeRequestService {
    private final MedicalRecordChangeRequestRepository requestRepository;
    private final MedicalRecordRepository recordRepository;
    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final MedicalRecordService medicalRecordService;

    public MedicalRecordChangeRequestService(MedicalRecordChangeRequestRepository requestRepository,
                                             MedicalRecordRepository recordRepository,
                                             DoctorRepository doctorRepository, UserRepository userRepository,
                                             MedicalRecordService medicalRecordService) {
        this.requestRepository = requestRepository;
        this.recordRepository = recordRepository;
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
        this.medicalRecordService = medicalRecordService;
    }

    @Transactional
    public MedicalRecordChangeResponseDTO submit(Long recordId, MedicalRecordChangeRequestDTO request,
                                                 String managerUsername) {
        MedicalRecord record = recordRepository.findByIdForUpdate(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Medical record not found."));
        User manager = userRepository.findByUsername(managerUsername)
                .filter(user -> user.getRole() == UserRole.DOCTOR_RECORDS_MANAGER)
                .orElseThrow(() -> new AccessDeniedException("Only a Doctor & Medical Record Manager can submit changes."));
        if (requestRepository.existsByMedicalRecordIdAndStatus(recordId, MedicalRecordChangeStatus.PENDING)) {
            throw new BadRequestException("A change request for this record is already awaiting doctor review.");
        }
        if (request.getChangeType() == MedicalRecordChangeType.EDIT
                && (request.getDiagnosis() == null || request.getDiagnosis().isBlank())) {
            throw new BadRequestException("A diagnosis is required for an edit request.");
        }
        MedicalRecordChangeRequest changeRequest = new MedicalRecordChangeRequest(
                record.getId(), record.getDoctor().getId(), manager, request.getChangeType(),
                request.getChangeType() == MedicalRecordChangeType.EDIT ? request.getDiagnosis().trim() : null,
                request.getChangeType() == MedicalRecordChangeType.EDIT ? request.getTreatment() : null,
                request.getChangeType() == MedicalRecordChangeType.EDIT ? request.getNotes() : null,
                record.getDiagnosis(), record.getTreatment(), record.getNotes(),
                request.getRequestReason(), LocalDateTime.now());
        return response(requestRepository.save(changeRequest));
    }

    @Transactional
    public void deleteRecordAsManager(Long recordId, String managerUsername) {
        User manager = userRepository.findByUsername(managerUsername)
                .filter(user -> user.getRole() == UserRole.DOCTOR_RECORDS_MANAGER)
                .orElseThrow(() -> new AccessDeniedException("Only a Doctor & Medical Record Manager can delete records."));
        MedicalRecord record = recordRepository.findByIdForUpdate(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Medical record not found."));
        requestRepository.rejectPendingForRecord(recordId, MedicalRecordChangeStatus.PENDING,
                MedicalRecordChangeStatus.REJECTED, manager, LocalDateTime.now(),
                "Closed because the Doctor & Medical Record Manager deleted the record directly.");
        recordRepository.delete(record);
    }

    @Transactional(readOnly = true)
    public List<MedicalRecordChangeResponseDTO> listForDoctor(String doctorUsername) {
        Doctor doctor = doctorRepository.findByUserUsername(doctorUsername)
                .filter(value -> Boolean.TRUE.equals(value.getActive()))
                .orElseThrow(() -> new ResourceNotFoundException("Approved doctor profile not found."));
        return requestRepository.findByDoctorIdOrderByRequestedAtDesc(doctor.getId())
                .stream().map(this::response).toList();
    }

    @Transactional
    public MedicalRecordChangeResponseDTO review(Long requestId, MedicalRecordChangeReviewDTO review,
                                                  String doctorUsername) {
        if (review.getStatus() != MedicalRecordChangeStatus.APPROVED
                && review.getStatus() != MedicalRecordChangeStatus.REJECTED) {
            throw new BadRequestException("A change request can only be approved or rejected.");
        }
        Doctor doctor = doctorRepository.findByUserUsername(doctorUsername)
                .filter(value -> Boolean.TRUE.equals(value.getActive()))
                .orElseThrow(() -> new ResourceNotFoundException("Approved doctor profile not found."));
        MedicalRecordChangeRequest request = requestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Medical record change request not found."));
        if (!doctor.getId().equals(request.getDoctorId())) {
            throw new ResourceNotFoundException("Medical record change request not found.");
        }
        if (request.getStatus() != MedicalRecordChangeStatus.PENDING) {
            throw new BadRequestException("This medical record change request has already been reviewed.");
        }
        User reviewer = userRepository.findByUsername(doctorUsername)
                .filter(user -> user.getRole() == UserRole.DOCTOR)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor account not found."));
        request.setReviewedBy(reviewer);
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewReason(review.getReviewReason() == null || review.getReviewReason().isBlank()
                ? null : review.getReviewReason().trim());

        if (review.getStatus() == MedicalRecordChangeStatus.APPROVED) {
            MedicalRecord record = recordRepository.findByIdForUpdate(request.getMedicalRecordId())
                    .orElseThrow(() -> new ResourceNotFoundException("The medical record no longer exists."));
            if (!doctor.getId().equals(record.getDoctor().getId())) {
                throw new ResourceNotFoundException("Medical record change request not found.");
            }
            if (!same(record.getDiagnosis(), request.getOriginalDiagnosis())
                    || !same(record.getTreatment(), request.getOriginalTreatment())
                    || !same(record.getNotes(), request.getOriginalNotes())) {
                request.setStatus(MedicalRecordChangeStatus.REJECTED);
                request.setReviewReason("The record changed after this request was submitted. Submit a new request.");
                return response(requestRepository.save(request));
            }
            if (request.getChangeType() == MedicalRecordChangeType.EDIT) {
                MedicalRecordDTO update = new MedicalRecordDTO();
                update.setDiagnosis(request.getDiagnosis());
                update.setTreatment(request.getTreatment());
                update.setNotes(request.getNotes());
                medicalRecordService.updateMedicalRecord(record.getId(), update);
            } else {
                recordRepository.delete(record);
            }
        }
        request.setStatus(review.getStatus());
        return response(requestRepository.save(request));
    }

    private boolean same(String first, String second) {
        return java.util.Objects.equals(first, second);
    }

    private MedicalRecordChangeResponseDTO response(MedicalRecordChangeRequest request) {
        String patientName = recordRepository.findById(request.getMedicalRecordId())
                .map(record -> record.getPatient().getName()).orElse("Record removed");
        return new MedicalRecordChangeResponseDTO(request, patientName);
    }
}
