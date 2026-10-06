package com.hospital.hms.audit.repository;

import com.hospital.hms.audit.model.AuditLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);
    List<AuditLog> findByUsernameOrderByTimestampDesc(String username);
    List<AuditLog> findByModuleOrderByTimestampDesc(String module);
}
