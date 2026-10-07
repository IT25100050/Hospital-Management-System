package com.hospital.hms.laboratory.repository;

import com.hospital.hms.laboratory.model.LabReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LabReportRepository extends JpaRepository<LabReport, Long> {
    Optional<LabReport> findByLabTestId(Long labTestId);
}
