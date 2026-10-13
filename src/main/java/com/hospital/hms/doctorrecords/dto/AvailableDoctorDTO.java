package com.hospital.hms.doctorrecords.dto;

import com.hospital.hms.doctorrecords.model.Doctor;

import java.util.List;

public class AvailableDoctorDTO {
    private final Long id;
    private final String name;
    private final String specialty;
    private final String qualifications;
    private final Integer experience;
    private final List<String> availableSlots;

    public AvailableDoctorDTO(Doctor doctor, List<String> availableSlots) {
        id = doctor.getId();
        name = doctor.getName();
        specialty = doctor.getSpecialization();
        qualifications = doctor.getQualifications();
        experience = doctor.getExperience();
        this.availableSlots = availableSlots;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getSpecialty() { return specialty; }
    public String getQualifications() { return qualifications; }
    public Integer getExperience() { return experience; }
    public List<String> getAvailableSlots() { return availableSlots; }
}
