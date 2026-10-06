package com.hospital.hms.audit.service;

import com.hospital.hms.audit.dto.AuditLogDTO;
import com.hospital.hms.audit.model.AuditLog;
import com.hospital.hms.audit.repository.AuditLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public List<AuditLogDTO> getRecentLogs(int limit) {
        int safeLimit = (limit <= 0 || limit > 500) ? 100 : limit;
        Pageable pageable = PageRequest.of(0, safeLimit);
        return auditLogRepository.findAllByOrderByTimestampDesc(pageable).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AuditLogDTO> getLogsByUsername(String username) {
        return auditLogRepository.findByUsernameOrderByTimestampDesc(username).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AuditLogDTO> getLogsByModule(String module) {
        return auditLogRepository.findByModuleOrderByTimestampDesc(module).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private AuditLogDTO mapToDTO(AuditLog log) {
        return new AuditLogDTO(log.getId(), log.getUsername(), log.getModule(), log.getAction(), log.getDetails(), log.getTimestamp());
    }
}
