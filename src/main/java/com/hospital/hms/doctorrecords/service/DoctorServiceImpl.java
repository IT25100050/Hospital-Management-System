package com.hospital.hms.doctorrecords.service;

import com.hospital.hms.common.exception.BadRequestException;
import com.hospital.hms.common.exception.DuplicateResourceException;
import com.hospital.hms.common.exception.ResourceNotFoundException;
import com.hospital.hms.doctorrecords.dto.DoctorDTO;
import com.hospital.hms.doctorrecords.model.Doctor;
import com.hospital.hms.doctorrecords.repository.DoctorRepository;
import com.hospital.hms.doctorrecords.repository.MedicalRecordRepository;
import com.hospital.hms.hospitaladmin.model.Department;
import com.hospital.hms.hospitaladmin.repository.DepartmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;
    private final MedicalRecordRepository medicalRecordRepository;

    public DoctorServiceImpl(DoctorRepository doctorRepository, DepartmentRepository departmentRepository, MedicalRecordRepository medicalRecordRepository) {
        this.doctorRepository = doctorRepository;
        this.departmentRepository = departmentRepository;
        this.medicalRecordRepository = medicalRecordRepository;
    }

    @Override
    public DoctorDTO createDoctor(DoctorDTO dto) {
        if (doctorRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("Doctor with email '" + dto.getEmail() + "' already exists.");
        }

        Department department = null;
        if (dto.getDepartmentId() != null) {
            department = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + dto.getDepartmentId()));
        }

        Doctor doctor = new Doctor(dto.getName(), dto.getSpecialization(), dto.getEmail(), dto.getPhone(), department);
        Doctor saved = doctorRepository.save(doctor);
        return mapToDTO(saved);
    }

    @Override
    public DoctorDTO getDoctorById(Long id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id: " + id));
        return mapToDTO(doctor);
    }

    @Override
    public List<DoctorDTO> getAllDoctors() {
        return doctorRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<DoctorDTO> getDoctorsBySpecialization(String specialization) {
        return doctorRepository.findBySpecialization(specialization).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public DoctorDTO updateDoctor(Long id, DoctorDTO dto) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id: " + id));

        if (dto.getEmail() != null && !dto.getEmail().equals(doctor.getEmail())
                && doctorRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("Doctor with email '" + dto.getEmail() + "' already exists.");
        }

        if (dto.getDepartmentId() != null) {
            Department department = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + dto.getDepartmentId()));
            doctor.setDepartment(department);
        }

        doctor.setName(dto.getName());
        doctor.setSpecialization(dto.getSpecialization());
        doctor.setEmail(dto.getEmail());
        doctor.setPhone(dto.getPhone());

        Doctor updated = doctorRepository.save(doctor);
        return mapToDTO(updated);
    }

    @Override
    public void deleteDoctor(Long id) {
        if (!doctorRepository.existsById(id)) {
            throw new ResourceNotFoundException("Doctor not found with id: " + id);
        }

        // MedicalRecord.doctor is a non-nullable foreign key, so deleting a doctor who
        // already has records would otherwise crash with a raw DB constraint violation.
        if (!medicalRecordRepository.findByDoctorId(id).isEmpty()) {
            throw new BadRequestException(
                    "Cannot delete this doctor while they still have medical records on file. " +
                    "Reassign or remove those records first.");
        }

        doctorRepository.deleteById(id);
    }

    private DoctorDTO mapToDTO(Doctor doctor) {
        Long deptId = doctor.getDepartment() != null ? doctor.getDepartment().getId() : null;
        String deptName = doctor.getDepartment() != null ? doctor.getDepartment().getName() : null;
        return new DoctorDTO(doctor.getId(), doctor.getName(), doctor.getSpecialization(), doctor.getEmail(), doctor.getPhone(), deptId, deptName);
    }
}
