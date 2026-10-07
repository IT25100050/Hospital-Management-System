package com.hospital.hms.doctorrecords.repository;

import com.hospital.hms.doctorrecords.model.DoctorApplication;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DoctorApplicationRepository extends JpaRepository<DoctorApplication, Long> {
    boolean existsByEmail(String email);
    boolean existsByMedicalRegistrationNumber(String medicalRegistrationNumber);
    List<DoctorApplication> findAllByOrderByIdDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from DoctorApplication a where a.id = :id")
    Optional<DoctorApplication> findByIdForUpdate(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update DoctorApplication application set application.reviewedBy = null " +
            "where application.reviewedBy.id = :userId")
    int detachReviewer(@Param("userId") Long userId);
}
