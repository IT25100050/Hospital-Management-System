package com.hospital.hms.doctorrecords.service;

import com.hospital.hms.doctorrecords.dto.MedicalRecordDTO;

import java.util.List;

public interface MedicalRecordService {
    MedicalRecordDTO createMedicalRecord(MedicalRecordDTO dto);
    MedicalRecordDTO getMedicalRecordById(Long id);
    List<MedicalRecordDTO> getAllMedicalRecords();
    List<MedicalRecordDTO> getMedicalRecordsByPatient(Long patientId);
    List<MedicalRecordDTO> getMedicalRecordsByDoctor(Long doctorId);
    MedicalRecordDTO updateMedicalRecord(Long id, MedicalRecordDTO dto);
    void deleteMedicalRecord(Long id);
}
