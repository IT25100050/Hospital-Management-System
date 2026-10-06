package com.hospital.hms.patient.service;

import com.hospital.hms.patient.dto.PatientRequestDTO;
import com.hospital.hms.patient.dto.PatientResponseDTO;
import java.util.List;

public interface PatientService {
    PatientResponseDTO createPatient(PatientRequestDTO request);
    PatientResponseDTO getPatientById(Long id);
    List<PatientResponseDTO> getAllPatients();
    PatientResponseDTO updatePatient(Long id, PatientRequestDTO request);
    void deletePatient(Long id);
}
