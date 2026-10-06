package com.hospital.hms.laboratory.dto;

import com.hospital.hms.common.enums.TestStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class LabTestDTO {
    private Long id;
    @NotBlank(message = "Test name is required")
    private String testName;
    private String description;
    @NotNull(message = "Price is required")
    @Positive(message = "Price must be greater than zero")
    private Double price;

    @NotNull(message = "Patient ID is required")
    private Long patientId;
    private String patientName;
    private TestStatus status;

    public LabTestDTO() {}

    public LabTestDTO(Long id, String testName, String description, Double price, Long patientId, String patientName, TestStatus status) {
        this.id = id;
        this.testName = testName;
        this.description = description;
        this.price = price;
        this.patientId = patientId;
        this.patientName = patientName;
        this.status = status;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTestName() { return testName; }
    public void setTestName(String testName) { this.testName = testName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public TestStatus getStatus() { return status; }
    public void setStatus(TestStatus status) { this.status = status; }
}
