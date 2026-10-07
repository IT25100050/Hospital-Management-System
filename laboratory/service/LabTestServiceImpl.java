package com.hospital.hms.laboratory.service;

import com.hospital.hms.common.enums.TestStatus;
import com.hospital.hms.common.exception.ResourceNotFoundException;
import com.hospital.hms.laboratory.dto.LabReportDTO;
import com.hospital.hms.laboratory.dto.LabTestDTO;
import com.hospital.hms.laboratory.model.LabReport;
import com.hospital.hms.laboratory.model.LabTest;
import com.hospital.hms.laboratory.repository.LabReportRepository;
import com.hospital.hms.laboratory.repository.LabTestRepository;
import com.hospital.hms.patient.model.Patient;
import com.hospital.hms.patient.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LabTestServiceImpl implements LabTestService {

    private final LabTestRepository labTestRepository;
    private final LabReportRepository labReportRepository;
    private final PatientRepository patientRepository;

    public LabTestServiceImpl(LabTestRepository labTestRepository, LabReportRepository labReportRepository, PatientRepository patientRepository) {
        this.labTestRepository = labTestRepository;
        this.labReportRepository = labReportRepository;
        this.patientRepository = patientRepository;
    }

    @Override
    public LabTestDTO orderLabTest(LabTestDTO dto) {
        Patient patient = patientRepository.findById(dto.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + dto.getPatientId()));

        LabTest labTest = new LabTest(
                dto.getTestName(),
                dto.getDescription(),
                dto.getPrice(),
                patient,
                TestStatus.ORDERED
        );

        LabTest saved = labTestRepository.save(labTest);
        return mapToTestDTO(saved);
    }

    @Override
    public LabTestDTO getLabTestById(Long id) {
        LabTest labTest = labTestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lab test not found with id: " + id));
        return mapToTestDTO(labTest);
    }

    @Override
    public List<LabTestDTO> getAllLabTests() {
        return labTestRepository.findAll().stream()
                .map(this::mapToTestDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<LabTestDTO> getLabTestsByPatient(Long patientId) {
        return labTestRepository.findByPatientId(patientId).stream()
                .map(this::mapToTestDTO)
                .collect(Collectors.toList());
    }

    @Override
    public LabTestDTO updateLabTest(Long id, LabTestDTO dto) {
        LabTest labTest = labTestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lab test not found with id: " + id));

        if (dto.getPatientId() != null && !dto.getPatientId().equals(labTest.getPatient().getId())) {
            Patient patient = patientRepository.findById(dto.getPatientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + dto.getPatientId()));
            labTest.setPatient(patient);
        }
        if (dto.getTestName() != null) {
            labTest.setTestName(dto.getTestName());
        }
        if (dto.getDescription() != null) {
            labTest.setDescription(dto.getDescription());
        }
        if (dto.getPrice() != null) {
            labTest.setPrice(dto.getPrice());
        }
        if (dto.getStatus() != null) {
            labTest.setStatus(dto.getStatus());
        }

        LabTest updated = labTestRepository.save(labTest);
        return mapToTestDTO(updated);
    }

    @Override
    public LabTestDTO updateTestStatus(Long id, TestStatus status) {
        LabTest labTest = labTestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lab test not found with id: " + id));

        labTest.setStatus(status);
        LabTest updated = labTestRepository.save(labTest);
        return mapToTestDTO(updated);
    }

    @Override
    @Transactional
    public void deleteLabTest(Long id) {
        LabTest labTest = labTestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lab test not found with id: " + id));

        labReportRepository.findByLabTestId(id).ifPresent(labReportRepository::delete);
        labTestRepository.delete(labTest);
    }

    @Override
    public LabReportDTO createLabReport(Long testId, String resultDetails, String remarks) {
        LabTest labTest = labTestRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Lab test not found with id: " + testId));

        labTest.setStatus(TestStatus.COMPLETED);
        labTestRepository.save(labTest);

        LabReport report = new LabReport(labTest, resultDetails, remarks, LocalDateTime.now());
        LabReport saved = labReportRepository.save(report);

        return mapToReportDTO(saved);
    }

    @Override
    public LabReportDTO getReportByTestId(Long testId) {
        LabReport report = labReportRepository.findByLabTestId(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Lab report not found for test id: " + testId));
        return mapToReportDTO(report);
    }

    @Override
    public LabReportDTO updateLabReport(Long reportId, String resultDetails, String remarks) {
        LabReport report = labReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Lab report not found with id: " + reportId));

        if (resultDetails != null) {
            report.setResultDetails(resultDetails);
        }
        if (remarks != null) {
            report.setRemarks(remarks);
        }

        LabReport updated = labReportRepository.save(report);
        return mapToReportDTO(updated);
    }

    @Override
    public void deleteLabReport(Long reportId) {
        LabReport report = labReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Lab report not found with id: " + reportId));

        LabTest labTest = report.getLabTest();
        labReportRepository.delete(report);

        labTest.setStatus(TestStatus.IN_PROGRESS);
        labTestRepository.save(labTest);
    }

    private LabTestDTO mapToTestDTO(LabTest test) {
        return new LabTestDTO(
                test.getId(),
                test.getTestName(),
                test.getDescription(),
                test.getPrice(),
                test.getPatient().getId(),
                test.getPatient().getName(),
                test.getStatus()
        );
    }

    private LabReportDTO mapToReportDTO(LabReport report) {
        return new LabReportDTO(
                report.getId(),
                report.getLabTest().getId(),
                report.getLabTest().getTestName(),
                report.getLabTest().getPatient().getName(),
                report.getResultDetails(),
                report.getRemarks(),
                report.getGeneratedDate()
        );
    }
}
