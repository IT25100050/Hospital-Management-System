package com.hospital.hms.laboratory.model;

import com.hospital.hms.common.enums.TestStatus;
import com.hospital.hms.patient.model.Patient;
import jakarta.persistence.*;

@Entity
@Table(name = "lab_tests")
public class LabTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String testName;

    private String description;

    @Column(nullable = false)
    private Double price;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TestStatus status;

    public LabTest() {}

    public LabTest(String testName, String description, Double price, Patient patient, TestStatus status) {
        this.testName = testName;
        this.description = description;
        this.price = price;
        this.patient = patient;
        this.status = status;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public String getTestName() { return testName; }
    public void setTestName(String testName) { this.testName = testName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }
    public TestStatus getStatus() { return status; }
    public void setStatus(TestStatus status) { this.status = status; }
}
