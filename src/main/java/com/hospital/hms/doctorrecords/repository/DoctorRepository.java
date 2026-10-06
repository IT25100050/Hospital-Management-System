package com.hospital.hms.doctorrecords.repository;

import com.hospital.hms.doctorrecords.model.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    List<Doctor> findBySpecialization(String specialization);
    List<Doctor> findByDepartmentId(Long departmentId);
    boolean existsByEmail(String email);
}
