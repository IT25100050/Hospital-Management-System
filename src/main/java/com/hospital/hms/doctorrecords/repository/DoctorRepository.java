package com.hospital.hms.doctorrecords.repository;

import com.hospital.hms.doctorrecords.model.Doctor;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    List<Doctor> findBySpecialization(String specialization);
    List<Doctor> findByDepartmentId(Long departmentId);
    boolean existsByEmail(String email);
    boolean existsByMedicalRegistrationNumber(String medicalRegistrationNumber);
    @Query("select d from Doctor d where d.active = true")
    List<Doctor> findApprovedDoctors();
    Optional<Doctor> findByUserUsername(String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Doctor d where d.id = :id")
    Optional<Doctor> findByIdForUpdate(@Param("id") Long id);
}
