package com.hospital.hms.doctorrecords.service;

import com.hospital.hms.common.exception.ResourceNotFoundException;
import com.hospital.hms.doctorrecords.dto.MedicalRecordDTO;
import com.hospital.hms.doctorrecords.model.Doctor;
import com.hospital.hms.doctorrecords.model.MedicalRecord;
import com.hospital.hms.doctorrecords.repository.DoctorRepository;
import com.hospital.hms.doctorrecords.repository.MedicalRecordRepository;
import com.hospital.hms.patient.model.Patient;
import com.hospital.hms.patient.repository.PatientRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MedicalRecordServiceImpl implements MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    public MedicalRecordServiceImpl(MedicalRecordRepository medicalRecordRepository, PatientRepository patientRepository, DoctorRepository doctorRepository) {
        this.medicalRecordRepository = medicalRecordRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
    }

    @Override
    public MedicalRecordDTO createMedicalRecord(MedicalRecordDTO dto) {
        Patient patient = patientRepository.findById(dto.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + dto.getPatientId()));

        Doctor doctor = doctorRepository.findById(dto.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id: " + dto.getDoctorId()));

        MedicalRecord record = new MedicalRecord(
                patient,
                doctor,
                dto.getDiagnosis(),
                dto.getTreatment(),
                dto.getNotes(),
                LocalDateTime.now()
        );

        MedicalRecord saved = medicalRecordRepository.save(record);
        return mapToDTO(saved);
    }

    @Override
    public MedicalRecordDTO getMedicalRecordById(Long id) {
        MedicalRecord record = medicalRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medical record not found with id: " + id));
        return mapToDTO(record);
    }

    @Override
    public List<MedicalRecordDTO> getAllMedicalRecords() {
        return medicalRecordRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<MedicalRecordDTO> getMedicalRecordsByPatient(Long patientId) {
        return medicalRecordRepository.findByPatientId(patientId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<MedicalRecordDTO> getMedicalRecordsByDoctor(Long doctorId) {
        return medicalRecordRepository.findByDoctorId(doctorId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public MedicalRecordDTO updateMedicalRecord(Long id, MedicalRecordDTO dto) {
        MedicalRecord record = medicalRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medical record not found with id: " + id));

        if (dto.getPatientId() != null && !dto.getPatientId().equals(record.getPatient().getId())) {
            Patient patient = patientRepository.findById(dto.getPatientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + dto.getPatientId()));
            record.setPatient(patient);
        }

        if (dto.getDoctorId() != null && !dto.getDoctorId().equals(record.getDoctor().getId())) {
            Doctor doctor = doctorRepository.findById(dto.getDoctorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id: " + dto.getDoctorId()));
            record.setDoctor(doctor);
        }

        if (dto.getDiagnosis() != null) {
            record.setDiagnosis(dto.getDiagnosis());
        }
        if (dto.getTreatment() != null) {
            record.setTreatment(dto.getTreatment());
        }
        if (dto.getNotes() != null) {
            record.setNotes(dto.getNotes());
        }

        MedicalRecord updated = medicalRecordRepository.save(record);
        return mapToDTO(updated);
    }

    @Override
    public void deleteMedicalRecord(Long id) {
        if (!medicalRecordRepository.existsById(id)) {
            throw new ResourceNotFoundException("Medical record not found with id: " + id);
        }
        medicalRecordRepository.deleteById(id);
    }

    private MedicalRecordDTO mapToDTO(MedicalRecord record) {
        return new MedicalRecordDTO(
                record.getId(),
                record.getPatient().getId(),
                record.getPatient().getName(),
                record.getDoctor().getId(),
                record.getDoctor().getName(),
                record.getDiagnosis(),
                record.getTreatment(),
                record.getNotes(),
                record.getRecordDate()
        );
    }
}
