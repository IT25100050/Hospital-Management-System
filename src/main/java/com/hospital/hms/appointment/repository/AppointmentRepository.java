package com.hospital.hms.appointment.repository;

import com.hospital.hms.appointment.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Collection;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByPatientId(Long patientId);
    List<Appointment> findByDoctorId(Long doctorId);
    boolean existsByDoctorIdAndAppointmentTime(Long doctorId, LocalDateTime appointmentTime);
    boolean existsByPatientIdAndDoctorIdAndAppointmentTimeAndStatusIn(
            Long patientId, Long doctorId, LocalDateTime appointmentTime, Collection<com.hospital.hms.common.enums.AppointmentStatus> statuses);
    boolean existsByDoctorIdAndAppointmentTimeAndStatusIn(
            Long doctorId, LocalDateTime appointmentTime, Collection<com.hospital.hms.common.enums.AppointmentStatus> statuses);
    List<Appointment> findByDoctorIdAndAppointmentTimeBetweenAndStatusIn(
            Long doctorId, LocalDateTime start, LocalDateTime end, Collection<com.hospital.hms.common.enums.AppointmentStatus> statuses);
    List<Appointment> findByDoctorIdAndAppointmentTimeAndStatusIn(
            Long doctorId, LocalDateTime appointmentTime, Collection<com.hospital.hms.common.enums.AppointmentStatus> statuses);
    long countByAppointmentTimeBetween(LocalDateTime start, LocalDateTime end);
}
