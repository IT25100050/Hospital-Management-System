package com.hospital.hms.patient.dto;

public class PatientResponseDTO {
    private Long id;
    private String name;
    private String email;
    private String phoneNumber;

    public PatientResponseDTO(Long id, String name, String email, String phoneNumber) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    // Getters
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
}
