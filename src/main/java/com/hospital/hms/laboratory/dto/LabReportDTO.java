package com.hospital.hms.laboratory.dto;

import java.time.LocalDateTime;

public class LabReportDTO {
    private Long id;
    private Long labTestId;
    private String testName;
    private String patientName;
    private String resultDetails;
    private String remarks;
    private LocalDateTime generatedDate;

    public LabReportDTO() {}

    public LabReportDTO(Long id, Long labTestId, String testName, String patientName, String resultDetails, String remarks, LocalDateTime generatedDate) {
        this.id = id;
        this.labTestId = labTestId;
        this.testName = testName;
        this.patientName = patientName;
        this.resultDetails = resultDetails;
        this.remarks = remarks;
        this.generatedDate = generatedDate;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getLabTestId() { return labTestId; }
    public void setLabTestId(Long labTestId) { this.labTestId = labTestId; }
    public String getTestName() { return testName; }
    public void setTestName(String testName) { this.testName = testName; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public String getResultDetails() { return resultDetails; }
    public void setResultDetails(String resultDetails) { this.resultDetails = resultDetails; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public LocalDateTime getGeneratedDate() { return generatedDate; }
    public void setGeneratedDate(LocalDateTime generatedDate) { this.generatedDate = generatedDate; }
}
