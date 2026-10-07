package com.hospital.hms.laboratory.repository;

import com.hospital.hms.laboratory.model.LabTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LabTestRepository extends JpaRepository<LabTest, Long> {
    List<LabTest> findByPatientId(Long patientId);
}
