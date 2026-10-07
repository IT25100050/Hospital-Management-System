package com.hospital.hms.laboratory.service;

import com.hospital.hms.common.enums.TestStatus;
import com.hospital.hms.laboratory.dto.LabReportDTO;
import com.hospital.hms.laboratory.dto.LabTestDTO;

import java.util.List;

public interface LabTestService {
    LabTestDTO orderLabTest(LabTestDTO dto);
    LabTestDTO getLabTestById(Long id);
    List<LabTestDTO> getAllLabTests();
    List<LabTestDTO> getLabTestsByPatient(Long patientId);
    LabTestDTO updateLabTest(Long id, LabTestDTO dto);
    LabTestDTO updateTestStatus(Long id, TestStatus status);
    void deleteLabTest(Long id);

    LabReportDTO createLabReport(Long testId, String resultDetails, String remarks);
    LabReportDTO getReportByTestId(Long testId);
    LabReportDTO updateLabReport(Long reportId, String resultDetails, String remarks);
    void deleteLabReport(Long reportId);
}
