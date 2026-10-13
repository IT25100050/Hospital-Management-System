package com.hospital.hms.doctorrecords.service;

import com.hospital.hms.appointment.model.Appointment;
import com.hospital.hms.appointment.repository.AppointmentRepository;
import com.hospital.hms.common.enums.AppointmentStatus;
import com.hospital.hms.common.exception.BadRequestException;
import com.hospital.hms.common.exception.ResourceNotFoundException;
import com.hospital.hms.doctorrecords.dto.MedicalRecordCreateRequest;
import com.hospital.hms.doctorrecords.dto.MedicalRecordDTO;
import com.hospital.hms.doctorrecords.model.Doctor;
import com.hospital.hms.doctorrecords.model.MedicalRecord;
import com.hospital.hms.doctorrecords.repository.DoctorRepository;
import com.hospital.hms.doctorrecords.repository.MedicalRecordRepository;
import com.hospital.hms.doctorrecords.repository.MedicalRecordChangeRequestRepository;
import com.hospital.hms.doctorrecords.model.MedicalRecordChangeStatus;
import com.hospital.hms.patient.model.Patient;
import com.hospital.hms.patient.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MedicalRecordServiceImpl implements MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final MedicalRecordChangeRequestRepository changeRequestRepository;

    public MedicalRecordServiceImpl(MedicalRecordRepository medicalRecordRepository, PatientRepository patientRepository,
                                    DoctorRepository doctorRepository, AppointmentRepository appointmentRepository,
                                    MedicalRecordChangeRequestRepository changeRequestRepository) {
        this.medicalRecordRepository = medicalRecordRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.changeRequestRepository = changeRequestRepository;
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
    @Transactional
    public MedicalRecordDTO createForApprovedAppointment(Long appointmentId, String doctorUsername,
                                                         MedicalRecordCreateRequest request) {
        Doctor doctor = doctorRepository.findByUserUsername(doctorUsername)
                .filter(value -> Boolean.TRUE.equals(value.getActive()))
                .orElseThrow(() -> new ResourceNotFoundException("Approved doctor profile not found."));
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found."));
        if (!doctor.getId().equals(appointment.getDoctorId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You can only create records for appointments assigned to you.");
        }
        if (appointment.getStatus() != AppointmentStatus.APPROVED) {
            throw new BadRequestException("A medical record can only be created for an approved appointment.");
        }
        MedicalRecord record = new MedicalRecord(appointment.getPatient(), doctor,
                request.getDiagnosis().trim(), request.getTreatment(), request.getNotes(),
                LocalDateTime.now());
        return mapToDTO(medicalRecordRepository.save(record));
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
    @Transactional
    public MedicalRecordDTO updateOwnMedicalRecord(Long id, MedicalRecordDTO dto, String doctorUsername) {
        MedicalRecord record = findOwnRecordForUpdate(id, doctorUsername);
        closePendingRequests(record);
        if (dto.getPatientId() != null && !dto.getPatientId().equals(record.getPatient().getId())
                || dto.getDoctorId() != null && !dto.getDoctorId().equals(record.getDoctor().getId())) {
            throw new BadRequestException("Doctors cannot reassign the patient or doctor on a medical record.");
        }
        return updateRecord(record, dto);
    }

    @Override
    public MedicalRecordDTO updateMedicalRecord(Long id, MedicalRecordDTO dto) {
        MedicalRecord record = medicalRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medical record not found with id: " + id));
        return updateRecord(record, dto);
    }

    private MedicalRecordDTO updateRecord(MedicalRecord record, MedicalRecordDTO dto) {
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
    @Transactional
    public void deleteMedicalRecord(Long id) {
        if (!medicalRecordRepository.existsById(id)) {
            throw new ResourceNotFoundException("Medical record not found with id: " + id);
        }
        medicalRecordRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteOwnMedicalRecord(Long id, String doctorUsername) {
        MedicalRecord record = findOwnRecordForUpdate(id, doctorUsername);
        closePendingRequests(record);
        medicalRecordRepository.delete(record);
    }

    private void closePendingRequests(MedicalRecord record) {
        changeRequestRepository.rejectPendingForRecord(record.getId(),
                MedicalRecordChangeStatus.PENDING, MedicalRecordChangeStatus.REJECTED,
                record.getDoctor().getUser(), LocalDateTime.now(),
                "Closed because the doctor changed or deleted the record directly.");
    }

    private MedicalRecord findOwnRecordForUpdate(Long id, String doctorUsername) {
        Doctor doctor = doctorRepository.findByUserUsername(doctorUsername)
                .filter(value -> Boolean.TRUE.equals(value.getActive()))
                .orElseThrow(() -> new ResourceNotFoundException("Approved doctor profile not found."));
        MedicalRecord record = medicalRecordRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medical record not found."));
        if (!doctor.getId().equals(record.getDoctor().getId())) {
            throw new AccessDeniedException("You can only change medical records assigned to your doctor profile.");
        }
        return record;
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
