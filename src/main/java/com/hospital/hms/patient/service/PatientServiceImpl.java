package com.hospital.hms.patient.service;

import com.hospital.hms.appointment.repository.AppointmentRepository;
import com.hospital.hms.common.exception.BadRequestException;
import com.hospital.hms.common.exception.ResourceNotFoundException;
import com.hospital.hms.doctorrecords.repository.MedicalRecordRepository;
import com.hospital.hms.laboratory.repository.LabTestRepository;
import com.hospital.hms.patient.dto.PatientRequestDTO;
import com.hospital.hms.patient.dto.PatientResponseDTO;
import com.hospital.hms.patient.model.Patient;
import com.hospital.hms.patient.repository.PatientRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final AppointmentRepository appointmentRepository;
    private final LabTestRepository labTestRepository;

    public PatientServiceImpl(PatientRepository patientRepository,
                               MedicalRecordRepository medicalRecordRepository,
                               AppointmentRepository appointmentRepository,
                               LabTestRepository labTestRepository) {
        this.patientRepository = patientRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.appointmentRepository = appointmentRepository;
        this.labTestRepository = labTestRepository;
    }

    @Override
    public PatientResponseDTO createPatient(PatientRequestDTO request) {
        Patient patient = new Patient(request.getName(), request.getEmail(), request.getPhoneNumber());
        Patient saved = patientRepository.save(patient);
        return mapToDTO(saved);
    }

    @Override
    public PatientResponseDTO getPatientById(Long id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + id));
        return mapToDTO(patient);
    }

    @Override
    public List<PatientResponseDTO> getAllPatients() {
        return patientRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public PatientResponseDTO updatePatient(Long id, PatientRequestDTO request) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + id));

        patient.setName(request.getName());
        patient.setEmail(request.getEmail());
        patient.setPhoneNumber(request.getPhoneNumber());

        Patient updated = patientRepository.save(patient);
        return mapToDTO(updated);
    }

    @Override
    public void deletePatient(Long id) {
        if (!patientRepository.existsById(id)) {
            throw new ResourceNotFoundException("Patient not found with id: " + id);
        }

        // MedicalRecord.patient, Appointment.patient and LabTest.patient are all
        // non-nullable foreign keys, so deleting a patient with any of these on file
        // would otherwise crash with a raw DB constraint violation.
        boolean hasRecords = !medicalRecordRepository.findByPatientId(id).isEmpty();
        boolean hasAppointments = !appointmentRepository.findByPatientId(id).isEmpty();
        boolean hasLabTests = !labTestRepository.findByPatientId(id).isEmpty();

        if (hasRecords || hasAppointments || hasLabTests) {
            throw new BadRequestException(
                    "Cannot delete this patient while they still have medical records, appointments, " +
                    "or lab tests on file. Remove those first.");
        }

        patientRepository.deleteById(id);
    }

    private PatientResponseDTO mapToDTO(Patient patient) {
        return new PatientResponseDTO(patient.getId(), patient.getName(), patient.getEmail(), patient.getPhoneNumber());
    }
}
