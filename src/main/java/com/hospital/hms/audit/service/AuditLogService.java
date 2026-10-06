package com.hospital.hms.audit.service;

import com.hospital.hms.audit.dto.AuditLogDTO;

import java.util.List;

public interface AuditLogService {
    List<AuditLogDTO> getRecentLogs(int limit);
    List<AuditLogDTO> getLogsByUsername(String username);
    List<AuditLogDTO> getLogsByModule(String module);
}
