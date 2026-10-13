package com.hospital.hms.doctorrecords.service;

import com.hospital.hms.doctorrecords.dto.MedicalRecordDTO;
import com.hospital.hms.doctorrecords.dto.MedicalRecordCreateRequest;

import java.util.List;

public interface MedicalRecordService {
    MedicalRecordDTO createMedicalRecord(MedicalRecordDTO dto);
    MedicalRecordDTO createForApprovedAppointment(Long appointmentId, String doctorUsername,
                                                  MedicalRecordCreateRequest request);
    MedicalRecordDTO getMedicalRecordById(Long id);
    List<MedicalRecordDTO> getAllMedicalRecords();
    List<MedicalRecordDTO> getMedicalRecordsByPatient(Long patientId);
    List<MedicalRecordDTO> getMedicalRecordsByDoctor(Long doctorId);
    MedicalRecordDTO updateMedicalRecord(Long id, MedicalRecordDTO dto);
    MedicalRecordDTO updateOwnMedicalRecord(Long id, MedicalRecordDTO dto, String doctorUsername);
    void deleteMedicalRecord(Long id);
    void deleteOwnMedicalRecord(Long id, String doctorUsername);
}
