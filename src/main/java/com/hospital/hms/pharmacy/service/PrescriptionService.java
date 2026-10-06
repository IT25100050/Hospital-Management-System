package com.hospital.hms.pharmacy.service;

import com.hospital.hms.pharmacy.dto.PrescriptionDTO;
import java.util.List;

public interface PrescriptionService {
    PrescriptionDTO createPrescription(PrescriptionDTO dto);
    PrescriptionDTO getPrescriptionById(Long id);
    List<PrescriptionDTO> getAllPrescriptions();
    List<PrescriptionDTO> getPrescriptionsByPatient(Long patientId);
    List<PrescriptionDTO> getPrescriptionsByDoctor(Long doctorId);
    PrescriptionDTO updatePrescription(Long id, PrescriptionDTO dto);
    void deletePrescription(Long id);
    void fulfillPrescription(Long prescriptionId);
}
