package com.hospital.hms.doctorrecords.service;

import com.hospital.hms.doctorrecords.dto.DoctorDTO;

import java.util.List;

public interface DoctorService {
    DoctorDTO createDoctor(DoctorDTO dto);
    DoctorDTO getDoctorById(Long id);
    List<DoctorDTO> getAllDoctors();
    List<DoctorDTO> getDoctorsBySpecialization(String specialization);
    DoctorDTO updateDoctor(Long id, DoctorDTO dto);
    void deleteDoctor(Long id);
}
