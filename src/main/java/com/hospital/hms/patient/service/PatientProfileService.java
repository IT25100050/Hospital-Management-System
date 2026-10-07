package com.hospital.hms.patient.service;

import com.hospital.hms.patient.model.Patient;

public interface PatientProfileService {
    Patient getOrCreateForUser(String username);
}
