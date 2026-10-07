package com.hospital.hms.laboratory.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lab_reports")
public class LabReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "lab_test_id", nullable = false)
    private LabTest labTest;

    @Column(columnDefinition = "TEXT")
    private String resultDetails;

    private String remarks;

    private LocalDateTime generatedDate;

    public LabReport() {}

    public LabReport(LabTest labTest, String resultDetails, String remarks, LocalDateTime generatedDate) {
        this.labTest = labTest;
        this.resultDetails = resultDetails;
        this.remarks = remarks;
        this.generatedDate = generatedDate;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public LabTest getLabTest() { return labTest; }
    public void setLabTest(LabTest labTest) { this.labTest = labTest; }
    public String getResultDetails() { return resultDetails; }
    public void setResultDetails(String resultDetails) { this.resultDetails = resultDetails; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public LocalDateTime getGeneratedDate() { return generatedDate; }
    public void setGeneratedDate(LocalDateTime generatedDate) { this.generatedDate = generatedDate; }
}
