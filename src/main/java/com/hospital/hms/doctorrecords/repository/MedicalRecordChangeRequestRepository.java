package com.hospital.hms.doctorrecords.repository;

import com.hospital.hms.doctorrecords.model.MedicalRecordChangeRequest;
import com.hospital.hms.doctorrecords.model.MedicalRecordChangeStatus;
import com.hospital.hms.auth.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Repository
public interface MedicalRecordChangeRequestRepository extends JpaRepository<MedicalRecordChangeRequest, Long> {
    List<MedicalRecordChangeRequest> findByDoctorIdOrderByRequestedAtDesc(Long doctorId);
    boolean existsByMedicalRecordIdAndStatus(Long medicalRecordId,
                                               com.hospital.hms.doctorrecords.model.MedicalRecordChangeStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update MedicalRecordChangeRequest request set request.status = :rejected, " +
            "request.reviewedBy = :reviewer, request.reviewedAt = :reviewedAt, " +
            "request.reviewReason = :reviewReason " +
            "where request.medicalRecordId = :recordId and request.status = :pending")
    int rejectPendingForRecord(@Param("recordId") Long recordId,
                               @Param("pending") MedicalRecordChangeStatus pending,
                               @Param("rejected") MedicalRecordChangeStatus rejected,
                               @Param("reviewer") User reviewer,
                               @Param("reviewedAt") LocalDateTime reviewedAt,
                               @Param("reviewReason") String reviewReason);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from MedicalRecordChangeRequest request where request.id = :id")
    Optional<MedicalRecordChangeRequest> findByIdForUpdate(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update MedicalRecordChangeRequest request set request.requestedBy = null, " +
            "request.requestedByUsername = :username where request.requestedBy.id = :userId")
    int detachRequester(@Param("userId") Long userId, @Param("username") String username);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update MedicalRecordChangeRequest request set request.reviewedBy = null " +
            "where request.reviewedBy.id = :userId")
    int detachReviewer(@Param("userId") Long userId);
}
