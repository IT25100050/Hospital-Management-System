package com.hospital.hms.doctorrecords.dto;

import jakarta.validation.constraints.*;

public class DoctorApplicationRequest {
    @NotBlank @Size(max = 100)
    private String name;
    @NotBlank @Email @Size(max = 254)
    private String email;
    @NotBlank @Size(min = 8, max = 100)
    private String password;
    @NotBlank @Pattern(regexp = "^[0-9+() -]{7,20}$")
    private String phoneNumber;
    @NotBlank @Size(max = 100)
    private String medicalRegistrationNumber;
    @NotBlank @Size(max = 100)
    private String specialty;
    @NotBlank @Size(max = 500)
    private String qualifications;
    @NotNull @Min(0) @Max(80)
    private Integer experience;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getMedicalRegistrationNumber() { return medicalRegistrationNumber; }
    public void setMedicalRegistrationNumber(String medicalRegistrationNumber) { this.medicalRegistrationNumber = medicalRegistrationNumber; }
    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }
    public String getQualifications() { return qualifications; }
    public void setQualifications(String qualifications) { this.qualifications = qualifications; }
    public Integer getExperience() { return experience; }
    public void setExperience(Integer experience) { this.experience = experience; }
}
