package com.hospital.hms.doctorrecords.repository;

import com.hospital.hms.doctorrecords.model.MedicalRecord;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
    List<MedicalRecord> findByPatientId(Long patientId);
    List<MedicalRecord> findByDoctorId(Long doctorId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select record from MedicalRecord record where record.id = :id")
    java.util.Optional<MedicalRecord> findByIdForUpdate(@Param("id") Long id);
}
